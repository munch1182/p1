package com.munch1182.lib.bluetooth.le

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

data class BLECharacteristic(
    val uuid: String,
    val properties: List<String>,
    val value: String? = null,
)

data class BLEServiceInfo(
    val uuid: String,
    val name: String,
    val characteristics: List<BLECharacteristic>,
)

/**
 * 将 Android BluetoothGattService 列表转换为更易用的 BLEServiceInfo 列表。
 * 包括服务 UUID、服务名称（基于常见 UUID 映射）、以及服务内所有特征的信息。
 */
fun List<BluetoothGattService>.collectServiceInfo(): List<BLEServiceInfo> {
    return this.map { service ->
        BLEServiceInfo(
            uuid = service.uuid.toString(),
            name = getServiceName(service.uuid.toString()).asBleUuid2ShortIfCan(),
            characteristics = service.characteristics.map { characteristic ->
                @Suppress("DEPRECATION")
                BLECharacteristic(
                    uuid = characteristic.uuid.toString(),
                    properties = getPropertiesList(characteristic.properties),
                    value = characteristic.value?.let { bytes ->
                        // 尝试将字节数组转为 UTF-8 字符串，若无法解析则返回 hex 字符串
                        try {
                            String(bytes, Charsets.UTF_8)
                        } catch (e: Exception) {
                            bytes.joinToString("") { "%02x".format(it) }
                        }
                    }
                )
            }
        )
    }
}

/**
 * 将 UUID 转换为短格式（如果是标准 16-bit UUID 的 128-bit 表示）。
 * 例如：00001800-0000-1000-8000-00805f9b34fb -> "1800"
 * 非标准 UUID 保持原样（大写字符串）。
 */
fun String.asBleUuid2ShortIfCan(): String {
    val full = this.uppercase()
    val standardSuffix = "-0000-1000-8000-00805F9B34FB"
    return if (full.endsWith(standardSuffix)) {
        // 提取前缀部分，如 "00001800"
        val prefix = full.substringBefore(standardSuffix)
        // 去掉前导的 "0000"，保留后 4 位 16 进制数
        if (prefix.length == 8 && prefix.startsWith("0000")) {
            prefix.substring(4) // 返回 "1800"
        } else {
            prefix
        }
    } else {
        full
    }
}

/**
 * 根据服务 UUID 返回一个人类可读的服务名称。
 * 若无法识别，则返回 UUID 的字符串表示。
 */
private fun getServiceName(uuid: String): String {
    val knownServices = mapOf(
        "00001801-0000-1000-8000-00805f9b34fb" to "Generic Attribute",
        "00001800-0000-1000-8000-00805f9b34fb" to "Generic Access",
        "0000180a-0000-1000-8000-00805f9b34fb" to "Device Information",
        "0000180f-0000-1000-8000-00805f9b34fb" to "Battery Service",
        "00001805-0000-1000-8000-00805f9b34fb" to "Current Time Service",
        "00001802-0000-1000-8000-00805f9b34fb" to "Immediate Alert",
        "00001803-0000-1000-8000-00805f9b34fb" to "Link Loss",
        "00001804-0000-1000-8000-00805f9b34fb" to "Tx Power",
    )
    return knownServices[uuid] ?: uuid
}

/**
 * 将 Android 特征属性位掩码转换为字符串列表。
 * 属性常量参考：BluetoothGattCharacteristic.PROPERTY_*
 */
private fun getPropertiesList(properties: Int): List<String> {
    val props = mutableListOf<String>()
    if (properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) props.add("BROADCAST")
    if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) props.add("READ")
    if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) props.add("WRITE_NO_RESPONSE")
    if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props.add("WRITE")
    if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props.add("NOTIFY")
    if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) props.add("INDICATE")
    if (properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) props.add("SIGNED_WRITE")
    if (properties and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) props.add("EXTENDED_PROPS")
    return props
}