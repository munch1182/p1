package com.munch1182.lib.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import com.munch1182.lib.android.ARManager
import com.munch1182.lib.android.ARSyncManager
import com.munch1182.lib.android.AppHelper
import com.munch1182.lib.android.LifecycleBoundScope
import com.munch1182.lib.android.Log
import com.munch1182.lib.android.OnUpdateListener
import com.munch1182.lib.android.getParcelableCompat
import com.munch1182.lib.android.invokeOnCompletion
import com.munch1182.lib.bluetooth.BluetoothEventFlow.event
import com.munch1182.lib.bluetooth.BluetoothEventFlow.onOffState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蓝牙状态变更
 */
interface IBluetoothStateHelper : ARManager<OnUpdateListener<BlueReceiverState>>

/**
 * 监听蓝牙相关的系统广播通知, 以获取最新的蓝牙状态/设置状态
 */
class BluetoothReceiver : BroadcastReceiver(), IBluetoothStateHelper, ARManager<OnUpdateListener<BlueReceiverState>> by ARSyncManager() {

    companion object {
        private const val TAG = "BluetoothReceiver"
    }

    private var isRegistered = AtomicBoolean(false)

    /**
     * 注册蓝牙状态监听, 如果已注册, 则不会重复注册
     */
    fun register(ctx: Context = AppHelper, handler: Handler? = null) {
        if (isRegistered.getAndSet(true)) return
        ctx.registerReceiver(this, IntentFilter().apply {
            // 监听设备绑定
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            // 监听蓝牙开关
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }, null, handler)
    }

    /**
     * 取消注册蓝牙状态监听, 如果未注册, 则不会执行
     */
    fun unregister(ctx: Context = AppHelper) {
        if (!isRegistered.getAndSet(false)) return
        ctx.unregisterReceiver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val dev = intent?.getParcelableCompat(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        when (intent?.action) {
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val curr = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                val prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                update(BlueReceiverState.BondStateChanged(dev, BlueBondState.from(prev), BlueBondState.from(curr)))
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> update(BlueReceiverState.AclConnected(dev))
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> update(BlueReceiverState.AclDisconnected(dev))

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                update(BlueReceiverState.BlueOnOffStateChanged(BlueOnOffState.from(state)))
            }
        }
    }

    private fun update(state: BlueReceiverState) {
        Log.d(TAG, "onBlueStateChange: $state")
        forEach { it.onUpdate(state) }
    }
}

/**
 * 蓝牙状态变更事件
 *
 * 会自动动态注册[BluetoothReceiver]
 *
 * @see onOffState
 * @see event
 */
object BluetoothEventFlow {
    private val receiver = BluetoothReceiver()

    /**
     * 监听广播的生命周期应该是全局的, 即使因为某些原因取消了广播监听, 也不影响事件流
     */
    private val scope = AppHelper

    // 原始事件共享流（所有 BlueReceiverState 事件）
    private val _events = MutableSharedFlow<BlueReceiverState>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * 获取蓝牙状态变更事件流
     *
     * 可使用filterIsInstance<BlueReceiverState.XXX>来过滤出指定的事件
     *
     * @see BlueReceiverState
     */
    val event = _events.asSharedFlow()

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

    /**
     * 全局的蓝牙开关监听流
     */
    val onOffState: StateFlow<Boolean> = _events //
        .filterIsInstance<BlueReceiverState.BlueOnOffStateChanged>() //
        .map { it.state == BlueOnOffState.On } //
        .distinctUntilChanged() //
        .stateIn( //
            scope = scope, started = SharingStarted.Eagerly, initialValue = BluetoothEnv.adapter?.isEnabled ?: false
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