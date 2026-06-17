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
import com.munch1182.lib.android.Log
import com.munch1182.lib.android.OnUpdateListener
import com.munch1182.lib.android.getParcelableCompat
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
