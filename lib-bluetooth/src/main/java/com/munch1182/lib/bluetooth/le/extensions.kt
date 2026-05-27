package com.munch1182.lib.bluetooth.le

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 开启通知及其描述符的通用实现
 */
suspend fun BLEConnector.enableNotify(serverPattern: String, notifyPattern: String, descriptorPattern: String? = null, timeout: Duration = 500.milliseconds): Result<Unit> {
    val service = findService(serverPattern) ?: return Result.failure(Exception("未找到${serverPattern}服务"))
    val notify = findCharacteristic(service, notifyPattern) ?: return Result.failure(Exception("未找到${notifyPattern}特征值"))
    if (descriptorPattern != null) {
        val descriptor = findDescriptor(notify, descriptorPattern) ?: return Result.failure(Exception("未找到${descriptorPattern}描述符"))
        val notification = setNotification(notify, true)
        if (!notification) return Result.failure(Exception("开启通知失败"))
        writeDescriptor(descriptor, timeout = timeout) ?: return Result.failure(Exception("写入描述符失败"))
    }
    return Result.success(Unit)
}

/**
 * 注册协议
 *
 * 当设备连接后, 可以根据[BLEProtocol.isSupport]判断支持的协议
 */
fun <T : Any> IBLEDeviceManager<T>.registerProtocols(vararg protocol: BLEProtocol<T>) {
    protocol.forEach { registerProtocol(it) }
}