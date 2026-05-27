package com.munch1182.lib.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice

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
