package com.munch1182.lib.helper.blue

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.getParcelableCompat
import com.munch1182.lib.base.newLog
import com.munch1182.lib.helper.ARDefaultManager
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface IBluetoothAdapter {
    val bm: BluetoothManager?
    val adapter: BluetoothAdapter?
}

interface IBluetoothReceiverListener {
    fun addBluetoothStateListener(l: OnBluetoothStateChangeListener): IBluetoothReceiverListener
    fun removeBluetoothStateListener(l: OnBluetoothStateChangeListener): IBluetoothReceiverListener
    fun addBondStateListener(l: OnBluetoothBondStateChangeListener): IBluetoothReceiverListener
    fun removeBondStateListener(l: OnBluetoothBondStateChangeListener): IBluetoothReceiverListener
}

interface IBluetoothEnv : IBluetoothAdapter {

    /*
     * 该设备是否支持蓝牙
     */
    val isBlueSupport get() = adapter != null

    /**
     * 该设备是否支持ble
     */
    val isBleSupport get() = BluetoothHelperHelper.ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    /**
     * 蓝牙是否打开
     */
    @get:RequiresPermission(Manifest.permission.BLUETOOTH)
    val isBlueOn get() = adapter?.isEnabled ?: false

    /**
     * 是否支持批处理扫描
     *
     * 如果支持，可以在扫描中设置[android.bluetooth.le.ScanSettings.Builder.setReportDelay]大于0
     * 则会回调[android.bluetooth.le.ScanCallback.onBatchScanResults]
     */
    val isScanBatchingSupported get() = adapter?.isLePeriodicAdvertisingSupported ?: false

    /**
     * 获取已配对的设备
     */
    @get:RequiresPermission(allOf = [Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT])
    val bondDevs get() = adapter?.bondedDevices ?: emptySet()

    /***
     * 回调[profile]类型已连接的设备
     */
    fun connectDevs(profile: Int, l: BluetoothProfile.ServiceListener?): Boolean {
        return adapter?.getProfileProxy(BluetoothHelperHelper.ctx, l, profile) ?: false
    }

    val profiles: Array<Int>
        get() {
            val profiles = arrayListOf(BluetoothProfile.HEADSET, BluetoothProfile.A2DP, BluetoothProfile.GATT, BluetoothProfile.GATT_SERVER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) profiles.add(BluetoothProfile.HEARING_AID)
            return profiles.toTypedArray()
        }

    /**
     * 回调所有已连接的设备，一个蓝牙地址只会返回一次
     */
    fun allConnectedDevs(l: OnResultListener<Array<BluetoothDevice>>) {
        allConnected {
            val map = hashMapOf<String, BluetoothDevice>()
            it.values.forEach { arr -> arr.forEach { a -> map[a.address] = a } }
            l.onResult(map.values.toTypedArray())
        }
    }

    /**
     * 回调所有支持的类型及其附属的已连接设备
     */
    fun allConnected(l: OnResultListener<Map<Int, Array<BluetoothDevice>>>) {
        allConnectedDevsLoop(hashMapOf(), profiles, 0, l)
    }

    private fun allConnectedDevsLoop(devs: HashMap<Int, Array<BluetoothDevice>>, profiles: Array<Int>, index: Int = 0, l: OnResultListener<Map<Int, Array<BluetoothDevice>>>) {
        if (index >= profiles.size) return l.onResult(devs)
        val result = connectDevs(profiles[index], object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                val result = proxy?.connectedDevices?.toTypedArray() ?: emptyArray()
                devs[profile] = result
                allConnectedDevsLoop(devs, profiles, index + 1, l)
            }

            override fun onServiceDisconnected(profile: Int) {
            }
        })
        if (!result) allConnectedDevsLoop(devs, profiles, index + 1, l)
    }

    /**
     * 调用该intent即可通过系统提示用户打开蓝牙
     */
    fun enableBlueIntent() = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
}

object BluetoothEnv : IBluetoothEnv, IBluetoothReceiverListener {
    override val bm: BluetoothManager? by lazy { BluetoothHelperHelper.ctx.getSystemService(BluetoothManager::class.java) }
    override val adapter: BluetoothAdapter? get() = bm?.adapter

    private val receiverLock = ReentrantLock()
    private val receiver = BluetoothReceiver()
        get() = receiverLock.withLock { field }

    @SuppressLint("MissingPermission")
    private fun ensureRegisterBeforeAdd() {
        if (receiver.isRegistered) return
        receiver.register()
    }

    private fun ensureUnregisterIfNoOneFollowAfterRemove() {
        if (!receiver.isRegistered) return
        receiver.unregisterIfNoOneFollow()
    }

    override fun addBluetoothStateListener(l: OnBluetoothStateChangeListener): IBluetoothReceiverListener {
        ensureRegisterBeforeAdd()
        receiver.addBluetoothStateListener(l)
        return this
    }

    override fun removeBluetoothStateListener(l: OnBluetoothStateChangeListener): IBluetoothReceiverListener {
        receiver.removeBluetoothStateListener(l)
        ensureUnregisterIfNoOneFollowAfterRemove()
        return this
    }

    override fun addBondStateListener(l: OnBluetoothBondStateChangeListener): IBluetoothReceiverListener {
        ensureRegisterBeforeAdd()
        receiver.addBondStateListener(l)
        return this
    }

    override fun removeBondStateListener(l: OnBluetoothBondStateChangeListener): IBluetoothReceiverListener {
        receiver.removeBondStateListener(l)
        ensureUnregisterIfNoOneFollowAfterRemove()
        return this
    }
}

class BluetoothReceiver : BroadcastReceiver(), IBluetoothReceiverListener {

    private val log = BluetoothHelperHelper.log.newLog("BluetoothReceiver")
    var isRegistered = false
        private set

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    fun register(ctx: Context = BluetoothHelperHelper.ctx) {
        log.logStr("register BluetoothReceiver")
        ctx.registerReceiver(this, IntentFilter().apply {
            // 蓝牙打开关闭状态
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            // 蓝牙连接状态
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            // 蓝牙绑定
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            // 蓝牙绑定前的配对框
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            // 蓝牙acl链路状态
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)

            /*addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)
            addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)*/
            /*addAction(BluetoothDevice.ACTION_FOUND)
             addAction(BluetoothDevice.ACTION_NAME_CHANGED)
             addAction(BluetoothDevice.ACTION_CLASS_CHANGED)*/
        })
        isRegistered = true
    }

    fun unregisterIfNoOneFollow(ctx: Context = BluetoothHelperHelper.ctx) {
        log.logStr("unregisterIfNoneFollow")
        if (isNoOneFollow()) {
            log.logStr("unregister BluetoothReceiver")
            ctx.unregisterReceiver(this)
            isRegistered = false
        }
    }

    private val bluetoothState = ARDefaultManager<OnBluetoothStateChangeListener>()
    private val bondState = ARDefaultManager<OnBluetoothBondStateChangeListener>()

    private fun isNoOneFollow(): Boolean {
        return bondState.size == 0 && bluetoothState.size == 0
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        val dev = intent?.getParcelableCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

        when (action) {
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val state = BluetoothBondState.from(intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1))
                val prevState = BluetoothBondState.from(intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1))
                log.logStr("$action: bondState: ${dev?.address}: $prevState -> $state")
                bondState.forEach { it.onBondStateChange(dev, state, prevState) }
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = BluetoothState.from(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1))
                val prevState = BluetoothState.from(intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1))
                log.logStr("$action: connectionState: $prevState -> $state")
                bluetoothState.forEach { it.onStateChange(state, prevState) }
            }

            else -> {
                log.logStr("action: $action: ${dev?.address}")
            }
        }
    }

    override fun addBondStateListener(l: OnBluetoothBondStateChangeListener): BluetoothReceiver {
        bondState.add(l)
        return this
    }

    override fun removeBondStateListener(l: OnBluetoothBondStateChangeListener): BluetoothReceiver {
        bondState.remove(l)
        return this
    }

    override fun addBluetoothStateListener(l: OnBluetoothStateChangeListener): IBluetoothReceiverListener {
        bluetoothState.add(l)
        return this
    }

    override fun removeBluetoothStateListener(l: OnBluetoothStateChangeListener): IBluetoothReceiverListener {
        bluetoothState.remove(l)
        return this
    }
}

sealed class BluetoothBondState(val value: Int) {
    data object BONDED : BluetoothBondState(BluetoothDevice.BOND_BONDED)
    data object BONDING : BluetoothBondState(BluetoothDevice.BOND_BONDING)
    data object NONE : BluetoothBondState(BluetoothDevice.BOND_NONE)

    companion object {
        fun from(value: Int) = when (value) {
            BluetoothDevice.BOND_BONDED -> BONDED
            BluetoothDevice.BOND_BONDING -> BONDING
            BluetoothDevice.BOND_NONE -> NONE
            else -> null
        }
    }

    val isBonded get() = this == BONDED
    val isBonding get() = this == BONDING
    val isNone get() = this == NONE
}

sealed class BluetoothState(val value: Int) {
    data object ON : BluetoothState(BluetoothAdapter.STATE_ON)
    data object OFF : BluetoothState(BluetoothAdapter.STATE_OFF)
    data object TURNING_ON : BluetoothState(BluetoothAdapter.STATE_TURNING_ON)
    data object TURNING_OFF : BluetoothState(BluetoothAdapter.STATE_TURNING_OFF)
    companion object {
        fun from(value: Int) = when (value) {
            BluetoothAdapter.STATE_ON -> ON
            BluetoothAdapter.STATE_OFF -> OFF
            BluetoothAdapter.STATE_TURNING_ON -> TURNING_ON
            BluetoothAdapter.STATE_TURNING_OFF -> TURNING_OFF
            else -> null
        }
    }

    val isOn get() = this == ON
    val isOff get() = this == OFF
    val isTurningOn get() = this == TURNING_ON
    val isTurningOff get() = this == TURNING_OFF
}

sealed class BluetoothConnectionState(val value: Int) {
    data object CONNECTING : BluetoothConnectionState(BluetoothAdapter.STATE_CONNECTING)
    data object CONNECTED : BluetoothConnectionState(BluetoothAdapter.STATE_CONNECTED)
    data object DISCONNECTING : BluetoothConnectionState(BluetoothAdapter.STATE_DISCONNECTING)
    data object DISCONNECTED : BluetoothConnectionState(BluetoothAdapter.STATE_DISCONNECTED)

    companion object {
        fun from(value: Int) = when (value) {
            BluetoothAdapter.STATE_CONNECTING -> CONNECTING
            BluetoothAdapter.STATE_CONNECTED -> CONNECTED
            BluetoothAdapter.STATE_DISCONNECTING -> DISCONNECTING
            BluetoothAdapter.STATE_DISCONNECTED -> DISCONNECTED
            else -> null
        }
    }

    val isConnecting get() = this == CONNECTING
    val isConnected get() = this == CONNECTED
    val isDisconnecting get() = this == DISCONNECTING
    val isDisconnected get() = this == DISCONNECTED
}

@FunctionalInterface
fun interface OnBluetoothBondStateChangeListener {
    fun onBondStateChange(dev: BluetoothDevice?, curr: BluetoothBondState?, prev: BluetoothBondState?)
}

@FunctionalInterface
fun interface OnBluetoothStateChangeListener {
    fun onStateChange(curr: BluetoothState?, prev: BluetoothState?)
}

@FunctionalInterface
fun interface OnBluetoothConnectStateChangeListener {
    fun onConnectStateChange(dev: BluetoothDevice?, curr: BluetoothConnectionState?, prev: BluetoothConnectionState?)
}