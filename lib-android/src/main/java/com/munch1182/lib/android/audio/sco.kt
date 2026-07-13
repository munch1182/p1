package com.munch1182.lib.android.audio

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.munch1182.lib.android.AppHelper
import com.munch1182.lib.android.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * SCO 连接状态（由 Manager 内部维护）
 */
enum class ScoState {
    Disconnected,  // 未连接
    Connecting,    // 正在尝试连接
    Connected      // 已连接
}

/**
 * 蓝牙 SCO 音频管理器（兼容 Android 全版本）
 * - 内部维护 ScoState 枚举，包含 Connecting 状态
 * - 监听系统真实连接状态（仅 Connected / Disconnected）
 * - 启动时若中途断开则立即失败（不等待超时）
 */
class BluetoothScoAudioManager(
    scope: CoroutineScope = AppHelper,
    private val app: Application = AppHelper.app
) {
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val log = logger()

    // 内部状态流，包含 Connecting
    private val _scoState = MutableStateFlow(ScoState.Disconnected)
    val scoState: StateFlow<ScoState> = _scoState.asStateFlow()

    // 互斥锁防止并发操作
    private val mutex = Mutex()

    init {
        // 监听系统状态（Boolean），更新为 Connected / Disconnected
        scope.launch {
            log.d("开始监听sco状态")
            listenScoState(app, audioManager).collect { isConnected ->
                log.d("sco状态变化：$isConnected")
                _scoState.value = if (isConnected) ScoState.Connected else ScoState.Disconnected
            }
        }
    }

    /**
     * 启动 SCO 并等待连接成功
     * @param timeout 超时时间
     * @return Result.success(Unit) 表示连接成功；失败携带具体异常
     */
    suspend fun startSco(timeout: Duration = 500.milliseconds): Result<Unit> {
        return withContext(Dispatchers.IO) {
            log.d("启动sco，timeout($timeout), currState(${_scoState.value})")
            mutex.withLock {
                // 如果已经连接，直接成功
                if (_scoState.value == ScoState.Connected) {
                    return@withLock Result.success(Unit)
                }

                // 设置状态为“连接中”
                _scoState.value = ScoState.Connecting
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

                // 执行底层启动
                val startResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    log.d("执行S版本开启逻辑")
                    startScoApiS()
                } else {
                    log.d("执行旧版本开启逻辑")
                    startScoApi()
                }
                log.d("执行结果：$startResult")

                // 若启动失败（如设备不存在、set失败），恢复状态并返回失败
                if (startResult.isFailure) {
                    _scoState.value = ScoState.Disconnected
                    return@withLock Result.failure(startResult.exceptionOrNull()!!)
                }
                // 等待连接成功，同时监控中途断开
                val waitConnected = wait4Connected(timeout)
                log.d("等待结果：$waitConnected")
                return@withLock waitConnected
            }
        }
    }

    /**
     * 停止 SCO 连接
     */
    suspend fun stopSco(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val targetRecoveryDevice = audioManager.availableCommunicationDevices.firstOrNull { dev ->
                            dev.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE // 先寻找1
                        } ?: audioManager.availableCommunicationDevices.firstOrNull { dev ->
                            dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER // 兼容
                        }
                        if (targetRecoveryDevice != null) {
                            // 某些手机使用clear会导致其类型转为8(而不是常规的1), 可以使用此方法强行设置
                            audioManager.setCommunicationDevice(targetRecoveryDevice)
                        } else {
                            audioManager.clearCommunicationDevice()
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        audioManager.stopBluetoothSco()
                    }

                    // 🎯 核心修正：断开时统一将系统音频 Mode 还原
                    audioManager.mode = AudioManager.MODE_NORMAL

                    _scoState.value = ScoState.Disconnected
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(Error.SysException(e).asException())
                }
            }
        }
    }

    /**
     * 等待状态变为 Connected，若变为 Disconnected 则立即失败（不等到超时）
     */
    private suspend fun wait4Connected(timeout: Duration): Result<Unit> {
        return withTimeoutOrNull(timeout) {
            // 等待状态变化，只要变为 Connected 或 Disconnected 就停止等待
            _scoState.first { state ->
                state == ScoState.Connected || state == ScoState.Disconnected
            }.let { finalState ->
                if (finalState == ScoState.Connected) {
                    Result.success(Unit)
                } else {
                    // 变为 Disconnected，说明连接失败（非超时）
                    Result.failure(Error.ConnectionFailed.asException())
                }
            }
        } ?: Result.failure(Error.Timeout2WaitState.asException())
    }

    private fun startScoApi() = try {
        @Suppress("DEPRECATION")
        audioManager.startBluetoothSco()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Error.SysException(e).asException())
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startScoApiS(): Result<Unit> {
        val device = getScoDevice()
            ?: return Result.failure(Error.ScoDeviceNotFound.asException())
        return try {
            val success = audioManager.setCommunicationDevice(device)
            if (success) Result.success(Unit) else Result.failure(Error.SetCommunicationDeviceFail.asException())
        } catch (e: Exception) {
            Result.failure(Error.SysException(e).asException())
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getScoDevice(): AudioDeviceInfo? {
        return audioManager.availableCommunicationDevices.firstOrNull { dev ->
            dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        }
    }

    class ScoStartException(val error: Error) : Exception(error.toString())

    sealed class Error {
        data class SysException(val cause: Exception) : Error() {
            override fun toString() = cause.message ?: cause.toString()
        }

        object Timeout2WaitState : Error() {
            override fun toString() = "Timeout waiting for SCO Connected"
        }

        object ConnectionFailed : Error() {
            override fun toString() = "SCO connection failed (state became Disconnected)"
        }

        object SetCommunicationDeviceFail : Error() {
            override fun toString() = "setCommunicationDevice returned false"
        }

        object ScoDeviceNotFound : Error() {
            override fun toString() = "No Bluetooth SCO/BLE headset device found"
        }

        fun asException() = ScoStartException(this)
    }
}

/**
 * 监听系统 SCO 状态（仅反映真实连接状态）
 * @return Flow<Boolean>，true=已连接，false=未连接
 */
fun listenScoState(
    app: Application,
    am: AudioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
    executor: Executor = Dispatchers.Default.asExecutor(),
): Flow<Boolean> = callbackFlow {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        fun isTargetDevice(device: AudioDeviceInfo?): Boolean {
            return device != null && (
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                    )
        }

        val listener = AudioManager.OnCommunicationDeviceChangedListener { device ->
            trySend(isTargetDevice(device))
        }

        am.addOnCommunicationDeviceChangedListener(executor, listener)
        trySend(isTargetDevice(am.communicationDevice))

        awaitClose {
            am.removeOnCommunicationDeviceChangedListener(listener)
        }
    } else {
        @Suppress("DEPRECATION")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED) {
                    val state = intent.getIntExtra(
                        AudioManager.EXTRA_SCO_AUDIO_STATE,
                        AudioManager.SCO_AUDIO_STATE_ERROR
                    )
                    when (state) {
                        AudioManager.SCO_AUDIO_STATE_CONNECTED -> trySend(true)
                        AudioManager.SCO_AUDIO_STATE_DISCONNECTED,
                        AudioManager.SCO_AUDIO_STATE_ERROR -> trySend(false)
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        app.registerReceiver(receiver, filter)
        @Suppress("DEPRECATION")
        trySend(am.isBluetoothScoOn)

        awaitClose {
            try {
                app.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}.distinctUntilChanged()