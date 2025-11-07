package com.munch1182.p1.base

import com.munch1182.lib.bluetooth.le.BLEConnector
import java.util.UUID

suspend fun BLEConnector.initBlue(): IniResult {
    val services = runCatching { discoverServices().takeIf { it.isSuccess } }.getOrNull() ?: return IniResult.NoDiscoverServices
    val service = services.services.find { it.uuid.startsWith("0000fff0") } ?: return IniResult.NoFindUUIDService
    val writer = service.characteristics.find { it.uuid.startsWith("0000b03f") } ?: return IniResult.NoFindUUIDWrite
    val notify = service.characteristics.find { it.uuid.startsWith("0000b03e") } ?: return IniResult.NoFindUUIDNotify
    val notifyUUID = notify.descriptors.find { it.uuid.startsWith("00002902") } ?: return IniResult.NoFindUUIDNotifyUUID
    val result = runCatching { setCharacteristicNotification(notify, true, notifyUUID.uuid) }.getOrNull()
    if (result == null || !result) return IniResult.NoSetNotify
    BleSender.register(this, writer)
    return IniResult.Success
}

private fun UUID.startsWith(any: String): Boolean {
    return toString().lowercase().startsWith(any.lowercase())
}