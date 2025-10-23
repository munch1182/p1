package com.munch1182.lib.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.getParcelableCompat
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.ARDefaultSyncManager
import com.munch1182.lib.helper.ARManager
import com.munch1182.lib.base.bm as bm2

interface IBluetoothEnv {
    val bm: BluetoothManager?
    val adapter: BluetoothAdapter? get() = bm?.adapter
}

object BluetoothIBluetoothEnv : IBluetoothEnv {
    private val _bm by lazy { bm2 }
    override val bm: BluetoothManager? get() = _bm
}

/**
 * 蓝牙状态监听
 *
 * @see register
 */
class BluetoothReceiver : BroadcastReceiver(), ARManager<BluetoothReceiver.OnBlueStateChange> by ARDefaultSyncManager() {
    private val log = log()
    fun register(ctx: Context = AppHelper, handler: Handler? = null) {
        ctx.registerReceiver(this, IntentFilter().apply {
            // 监听设备绑定
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            // 监听蓝牙开关
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }, null, handler)
    }

    fun unregister(ctx: Context = AppHelper) {
        clear()
        try {
            ctx.unregisterReceiver(this)
        } catch (_: Exception) {
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val dev = intent?.getParcelableCompat(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        when (intent?.action) {
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val curr = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                val prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                update(BlueState.BondStateChanged(dev, BlueBondState.from(prev), BlueBondState.from(curr)))
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> update(BlueState.AclConnected(dev))
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> update(BlueState.AclDisconnected(dev))

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                update(BlueState.BlueStateChanged(BlueSysState.from(state)))
            }
        }
    }

    private fun update(state: BlueState) {
        log.logStr("onBlueStateChange: $state")
        forEach { it.onBlueStateChange(state) }
    }

    fun interface OnBlueStateChange {
        fun onBlueStateChange(state: BlueState)
    }

    sealed class BlueBondState(val value: Int) {
        object BondNone : BlueBondState(BluetoothDevice.BOND_NONE)
        object Bonding : BlueBondState(BluetoothDevice.BOND_BONDING)
        object Bonded : BlueBondState(BluetoothDevice.BOND_BONDED)

        override fun toString() = when (this) {
            BondNone -> "BondNone"
            Bonded -> "Bonded"
            Bonding -> "Bonding"
        }

        companion object {
            fun from(state: Int) = when (state) {
                BluetoothDevice.BOND_NONE -> BondNone
                BluetoothDevice.BOND_BONDED -> Bonded
                BluetoothDevice.BOND_BONDING -> Bonding
                else -> null
            }
        }
    }

    sealed class BlueSysState(val value: Int) {
        object Off : BlueSysState(BluetoothAdapter.STATE_OFF)
        object On : BlueSysState(BluetoothAdapter.STATE_ON)
        object TurningOn : BlueSysState(BluetoothAdapter.STATE_TURNING_ON)
        object TurningOff : BlueSysState(BluetoothAdapter.STATE_TURNING_OFF)

        override fun toString() = when (this) {
            Off -> "OFF"
            On -> "ON"
            TurningOff -> "TURNING_ON"
            TurningOn -> "TURNING_OFF"
        }

        companion object {
            fun from(state: Int) = when (state) {
                BluetoothAdapter.STATE_OFF -> Off
                BluetoothAdapter.STATE_ON -> On
                BluetoothAdapter.STATE_TURNING_ON -> TurningOn
                BluetoothAdapter.STATE_TURNING_OFF -> TurningOff
                else -> null
            }
        }
    }

    sealed class BlueState {
        class BondStateChanged(val dev: BluetoothDevice?, val curr: BlueBondState?, val prev: BlueBondState?) : BlueState()
        class AclConnected(val dev: BluetoothDevice?) : BlueState()
        class AclDisconnected(val dev: BluetoothDevice?) : BlueState()
        class BlueStateChanged(val state: BlueSysState?) : BlueState()

        override fun toString() = when (this) {
            is BondStateChanged -> "BondStateChanged(${dev?.address}):  $curr => $prev"
            is AclConnected -> "AclConnected(${dev?.address})"
            is AclDisconnected -> "AclDisconnected(${dev?.address})"
            is BlueStateChanged -> "BlueStateChanged($state)"
        }
    }
}