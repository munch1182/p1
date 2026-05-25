package com.munch1182.lib.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import androidx.annotation.RequiresPermission
import com.munch1182.core.android.ARManager
import com.munch1182.core.android.ARSyncManager
import com.munch1182.core.android.AppHelper
import com.munch1182.core.android.Log
import com.munch1182.core.android.OnUpdateListener
import com.munch1182.core.android.getParcelableCompat
import java.util.concurrent.atomic.AtomicBoolean

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
}

/**
 * 实现一个[IBluetoothEnv]
 */
object BluetoothIBluetoothEnv : IBluetoothEnv {
    private val _bm by lazy { AppHelper.getSystemService(BluetoothManager::class.java) }
    override val bm: BluetoothManager? get() = _bm
}

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
 * 蓝牙绑定状态
 *
 * @param value 系统值
 */
sealed class BlueBondState(val value: Int) {
    /**
     * 未绑定
     */
    object BondNone : BlueBondState(BluetoothDevice.BOND_NONE)

    /**
     * 绑定中
     */
    object Bonding : BlueBondState(BluetoothDevice.BOND_BONDING)

    /**
     * 已绑定
     */
    object Bonded : BlueBondState(BluetoothDevice.BOND_BONDED)

    /**
     * 绑定简化判断
     */
    val isBonded get() = this == Bonded

    /**
     * 未绑定简化判断
     */
    val isBondNone get() = this == BondNone

    /**
     * 绑定中简化判断
     */
    val isNotBonding get() = this != Bonding

    override fun toString() = when (this) {
        BondNone -> "BondNone"
        Bonded -> "Bonded"
        Bonding -> "Bonding"
    }

    companion object {
        /**
         * 将系统值转为此类型, 不符合在返回null
         */
        fun from(state: Int) = when (state) {
            BluetoothDevice.BOND_NONE -> BondNone
            BluetoothDevice.BOND_BONDED -> Bonded
            BluetoothDevice.BOND_BONDING -> Bonding
            else -> null
        }
    }
}

/**
 * 蓝牙系统开关状态
 *
 * @param value 系统值
 */
sealed class BlueOnOffState(val value: Int) {
    /**
     * 蓝牙已关闭
     */
    object Off : BlueOnOffState(BluetoothAdapter.STATE_OFF)

    /**
     * 蓝牙已开启
     */
    object On : BlueOnOffState(BluetoothAdapter.STATE_ON)

    /**
     * 蓝牙正在打开
     */
    object TurningOn : BlueOnOffState(BluetoothAdapter.STATE_TURNING_ON)

    /**
     * 蓝牙正在关闭
     */
    object TurningOff : BlueOnOffState(BluetoothAdapter.STATE_TURNING_OFF)

    override fun toString() = when (this) {
        Off -> "OFF"
        On -> "ON"
        TurningOff -> "TURNING_OFF"
        TurningOn -> "TURNING_ON"
    }

    companion object {
        /**
         * 将系统值转为此类型, 不符合在返回null
         */
        fun from(state: Int) = when (state) {
            BluetoothAdapter.STATE_OFF -> Off
            BluetoothAdapter.STATE_ON -> On
            BluetoothAdapter.STATE_TURNING_ON -> TurningOn
            BluetoothAdapter.STATE_TURNING_OFF -> TurningOff
            else -> null
        }
    }
}

/**
 * 蓝牙广播状态
 */
sealed class BlueReceiverState {
    /**
     * 绑定状态变更
     */
    class BondStateChanged(val dev: BluetoothDevice?, val prev: BlueBondState?, val curr: BlueBondState?) : BlueReceiverState()

    /**
     * ACL连接状态变更
     */
    class AclConnected(val dev: BluetoothDevice?) : BlueReceiverState()

    /**
     * ACL断开连接
     */
    class AclDisconnected(val dev: BluetoothDevice?) : BlueReceiverState()

    /**
     * 蓝牙开关状态变更
     */
    class BlueOnOffStateChanged(val state: BlueOnOffState?) : BlueReceiverState()

    /**
     * 简化判断
     */
    val isStateOff get() = this is BlueOnOffStateChanged && (state == BlueOnOffState.Off || state == BlueOnOffState.TurningOff)

    override fun toString() = when (this) {
        is BondStateChanged -> "BondStateChanged(${dev?.address}):  $prev => $curr"
        is AclConnected -> "AclConnected(${dev?.address})"
        is AclDisconnected -> "AclDisconnected(${dev?.address})"
        is BlueOnOffStateChanged -> "BlueStateChanged($state)"
    }
}