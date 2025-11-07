package com.munch1182.lib.bluetooth.le

import android.bluetooth.le.ScanRecord
import com.munch1182.lib.base.toBinaryStr
import com.munch1182.lib.base.toHexStr

object BlueScanRecordHelper {

    /**
     * @see android.bluetooth.le.ScanRecord#parseFromBytes(byte[])
     */
    fun parseScanRecord(byte: ByteArray): List<BlueRecord> {
        var index = 0
        val list = mutableListOf<BlueRecord>()
        while (index < byte.size) {
            val len = byte[index++]
            if (len == 0.toByte()) break
            val type = byte[index++]
            val newLen = len.toInt() - 1
            val value = byte.copyOfRange(index, index + newLen)
            index += newLen
            list.add(BlueRecord.new(len, type, value))
        }
        return list
    }

    sealed class BlueRecord(val len: Byte, val type: Byte, val value: ByteArray) {

        companion object {
            fun new(len: Byte, type: Byte, value: ByteArray): BlueRecord {
                return when (type.toInt()) {
                    ScanRecord.DATA_TYPE_FLAGS -> Flags(len, type, value)
                    ScanRecord.DATA_TYPE_LOCAL_NAME_SHORT, ScanRecord.DATA_TYPE_LOCAL_NAME_COMPLETE -> LocalName(len, type, value)
                    else -> Normal(len, type, value)
                }
            }
        }

        class Normal(len: Byte, type: Byte, value: ByteArray) : BlueRecord(len, type, value)

        /**
         * 位（Bit） 名称	                                含义	                                            值（0/1）
         * Bit 0	LE Limited Discoverable Mode	        设备处于 有限可发现模式（持续时间通常 ≤ 30秒）	    1 启用
         * Bit 1	LE General Discoverable Mode	        设备处于 通用可发现模式（持续可见，直到主动关闭）	    1 启用
         * Bit 2	BR/EDR Not Supported	                设备 不支持传统蓝牙（BR/EDR），仅支持低功耗（BLE）	1 不支持
         * Bit 3	Simultaneous LE & BR/EDR (Controller)	控制层 同时支持 BLE 和传统蓝牙（双模设备）	        1 支持
         * Bit 4	Simultaneous LE & BR/EDR (Host)	        主机层 同时支持 BLE 和传统蓝牙（双模设备）	        1 支持
         * Bit 5-7	Reserved	                            保留位（必须设为 0）
         */
        class Flags(len: Byte, type: Byte, value: ByteArray) : BlueRecord(len, type, value) {
            val flags: Byte = value[0]

            val flagStr get() = flags.toBinaryStr(fillZero = true)

            val typeStr get() = if (flagStr.reversed()[2] == '0') "CLASSIC and LE" else "LE only"
        }

        class LocalName(len: Byte, type: Byte, value: ByteArray) : BlueRecord(len, type, value) {
            val name: String = String(value)
        }

        override fun toString(): String {
            return "BlueRecord(${typeStr()}($type), [${value.toHexStr()}]"
        }

        fun value2StrIfTypeCan() = when (this) {
            is Flags -> "$typeStr($flagStr)"
            is LocalName -> name
            else -> value.toHexStr()
        }

        fun typeStr(): String {
            return when (type.toInt()) {
                ScanRecord.DATA_TYPE_NONE -> "DATA_TYPE_NONE"
                ScanRecord.DATA_TYPE_FLAGS -> "DATA_TYPE_FLAGS"
                ScanRecord.DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL -> "DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL"
                ScanRecord.DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE -> "DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE"
                ScanRecord.DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL -> "DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL"
                ScanRecord.DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE -> "DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE"
                ScanRecord.DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL -> "DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL"
                ScanRecord.DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE -> "DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE"
                ScanRecord.DATA_TYPE_LOCAL_NAME_SHORT -> "DATA_TYPE_LOCAL_NAME_SHORT"
                ScanRecord.DATA_TYPE_LOCAL_NAME_COMPLETE -> "DATA_TYPE_LOCAL_NAME_COMPLETE"
                ScanRecord.DATA_TYPE_TX_POWER_LEVEL -> "DATA_TYPE_TX_POWER_LEVEL"
                ScanRecord.DATA_TYPE_CLASS_OF_DEVICE -> "DATA_TYPE_CLASS_OF_DEVICE"
                ScanRecord.DATA_TYPE_SIMPLE_PAIRING_HASH_C -> "DATA_TYPE_SIMPLE_PAIRING_HASH_C"
                ScanRecord.DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R -> "DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R"
                ScanRecord.DATA_TYPE_DEVICE_ID -> "DATA_TYPE_DEVICE_ID"
                ScanRecord.DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS -> "DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS"
                ScanRecord.DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE -> "DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE"
                ScanRecord.DATA_TYPE_SERVICE_SOLICITATION_UUIDS_16_BIT -> "DATA_TYPE_SERVICE_SOLICITATION_UUIDS_16_BIT"
                ScanRecord.DATA_TYPE_SERVICE_SOLICITATION_UUIDS_32_BIT -> "DATA_TYPE_SERVICE_SOLICITATION_UUIDS_32_BIT"
                ScanRecord.DATA_TYPE_SERVICE_SOLICITATION_UUIDS_128_BIT -> "DATA_TYPE_SERVICE_SOLICITATION_UUIDS_128_BIT"
                ScanRecord.DATA_TYPE_SERVICE_DATA_16_BIT -> "DATA_TYPE_SERVICE_DATA_16_BIT"
                ScanRecord.DATA_TYPE_SERVICE_DATA_32_BIT -> "DATA_TYPE_SERVICE_DATA_32_BIT"
                ScanRecord.DATA_TYPE_SERVICE_DATA_128_BIT -> "DATA_TYPE_SERVICE_DATA_128_BIT"
                ScanRecord.DATA_TYPE_LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE -> "DATA_TYPE_LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE"
                ScanRecord.DATA_TYPE_LE_SECURE_CONNECTIONS_RANDOM_VALUE -> "DATA_TYPE_LE_SECURE_CONNECTIONS_RANDOM_VALUE"
                ScanRecord.DATA_TYPE_URI -> "DATA_TYPE_URI"
                ScanRecord.DATA_TYPE_INDOOR_POSITIONING -> "DATA_TYPE_INDOOR_POSITIONING"
                ScanRecord.DATA_TYPE_TRANSPORT_DISCOVERY_DATA -> "DATA_TYPE_TRANSPORT_DISCOVERY_DATA"
                ScanRecord.DATA_TYPE_MANUFACTURER_SPECIFIC_DATA -> "DATA_TYPE_MANUFACTURER_SPECIFIC_DATA"
                else -> type.toString()
            }
        }
    }
}