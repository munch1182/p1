package com.munch1182.lib.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.munch1182.lib.android.AppHelper
import com.munch1182.lib.android.LifecycleBoundScope
import com.munch1182.lib.android.OnUpdateListener
import com.munch1182.lib.android.invokeOnCompletion
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.coroutines.resume

/**
 * 提供蓝牙环境, 主要是[BluetoothManager]和[BluetoothAdapter]
 */
interface IBluetoothEnv {
    /**
     * 获取[BluetoothManager]
     */
    val bm: BluetoothManager?

    /**
     * 获取[BluetoothAdapter]
     */
    val adapter: BluetoothAdapter? get() = bm?.adapter

    /**
     * 获取gatt蓝牙设备, 需要蓝牙权限
     *
     * 如果要获取非gatt蓝牙设备 @see BluetoothProfileCache
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getConnectedDevs() = bm?.getConnectedDevices(BluetoothProfile.GATT)

    val onOffState: StateFlow<Boolean>

    val event: SharedFlow<BlueReceiverState>

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun createBond(dev: BluetoothDevice, timeout: Duration = 15000.milliseconds): Boolean
}

/**
 * 请求打开蓝牙开关的intent
 */
val enableBluetoothIntent get() = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

/**
 * 实现一个[IBluetoothEnv], 自动注册[BluetoothReceiver]监听蓝牙状态变更.
 *
 * 初始化由[BluetoothEnvInitializer]在ContentProvider阶段触发(Application.onCreate之前),
 * 保证在主线程确定执行, 避免object init的首次访问时机不确定性.
 *
 * @see BluetoothEnvInitializer
 */
object BluetoothEnv : IBluetoothEnv {
    private val _bm by lazy { AppHelper.getSystemService(BluetoothManager::class.java) }
    override val bm: BluetoothManager? get() = _bm

    private val receiver = BluetoothReceiver()

    /**
     * 监听广播的生命周期应该是全局的, 即使因为某些原因取消了广播监听, 也不影响事件流
     */
    private val scope = AppHelper

    private val _events = MutableSharedFlow<BlueReceiverState>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val event = _events.asSharedFlow()

    init {
        receiver.register()
        val listener = OnUpdateListener<BlueReceiverState> { event ->
            _events.tryEmit(event)
        }
        receiver.add(listener)

        // scope 取消时清理资源
        scope.invokeOnCompletion {
            receiver.remove(listener)
            receiver.unregister()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun createBond(dev: BluetoothDevice, timeout: Duration): Boolean {
        if (dev.bondState != BluetoothDevice.BOND_BONDED) return true
        return withTimeout(timeout) {
            suspendCancellableCoroutine { coroutine ->
                val job = scope.launch {
                    _events.filterIsInstance<BlueReceiverState.BondStateChanged>()
                        .filter { it.dev?.address == dev.address }
                        .collect { event ->
                            when (event.curr) {
                                BlueBondState.Bonded -> {
                                    if (coroutine.isActive) coroutine.resume(true)
                                    this.cancel()
                                }

                                BlueBondState.BondNone -> {
                                    if (coroutine.isActive) coroutine.resume(false)
                                    this.cancel()
                                }

                                else -> {}
                            }
                        }
                }
                coroutine.invokeOnCancellation { job.cancel() }
                // 监听就绪后再发起绑定
                if (dev.bondState != BluetoothDevice.BOND_BONDING) {
                    dev.createBond()
                }
            }
        }
    }

    /**
     * 全局的蓝牙开关监听流
     */
    override val onOffState: StateFlow<Boolean> = _events
        .filterIsInstance<BlueReceiverState.BlueOnOffStateChanged>()
        .map { it.state == BlueOnOffState.On }
        .distinctUntilChanged()
        .stateIn(
            scope = scope, started = SharingStarted.Eagerly,
            initialValue = adapter?.isEnabled ?: false
        )

    /**
     * 返回一个应用周期内, 蓝牙打开时有效关闭时无效的[LifecycleBoundScope];
     *
     * @see LifecycleBoundScope.currScopeOrEmpty 可以作为蓝牙活动的父scope, 以获取蓝牙关闭时自动关闭蓝牙连接/蓝牙发送的效果(但同时要处理对象的重建)
     */
    val onOffLifeBoundScope
        get() = LifecycleBoundScope(
            parentScope = AppHelper,
            isActiveFlow = onOffState,
            scopeContextAddition = SupervisorJob() + CoroutineName("BluetoothOnOffLifecycle")
        )
}
