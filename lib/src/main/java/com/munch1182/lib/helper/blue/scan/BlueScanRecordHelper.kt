package com.munch1182.lib.helper.blue.scan

import android.bluetooth.le.ScanRecord
import com.munch1182.lib.base.log
import com.munch1182.lib.base.toHexStr

object BlueScanRecordHelper {

    private val log = log()

    fun parseScanRecord(byte: ByteArray): MutableList<BlueRecord> {
        var index = 0
        val list = mutableListOf<BlueRecord>()
        while (index < byte.size) {
            val len = byte[index++]
            if (len == 0.toByte()) break
            val type = byte[index++]
            val newLen = len.toInt() - 1
            val value = byte.copyOfRange(index, index + newLen)
            index += newLen
            list.add(BlueRecord(len, type, value))
        }
        return list
    }

    class BlueRecord(val len: Byte, val type: Byte, val value: ByteArray) {

        override fun toString(): String {
            return "BlueRecord(${typeStr()}($type), [${value.toHexStr()}]"
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