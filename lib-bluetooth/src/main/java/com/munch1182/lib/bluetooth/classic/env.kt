package com.munch1182.lib.bluetooth.classic

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import com.munch1182.lib.android.AppHelper
import com.munch1182.lib.android.getParcelableArrayCompat
import com.munch1182.lib.android.getParcelableCompat
import kotlinx.coroutines.job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
suspend fun BluetoothDevice.isSupportSpp(timeout: Duration = 15000.milliseconds): Boolean {
    // 确保设备已配对（未配对的设备 fetchUuidsWithSdp 通常无效）
    val device = this
    if (device.bondState != BluetoothDevice.BOND_BONDED) return false

    return suspendCancellableCoroutine { continuation ->
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != BluetoothDevice.ACTION_UUID) return
                val dev = intent.getParcelableCompat(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                if (dev?.address != device.address) return

                val uuids = intent.getParcelableArrayCompat(BluetoothDevice.EXTRA_UUID, ParcelUuid::class.java)
                val isSupport = uuids?.any {
                    it.uuid?.toString()?.equals(SPP_DEFAULT_UUID.toString(), ignoreCase = true) == true
                } ?: false
                context?.unregisterReceiver(this)
                if (continuation.isActive) continuation.resume(isSupport)
            }
        }

        val appCtx = AppHelper
        appCtx.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_UUID))

        // 发起 SDP 查询
        device.fetchUuidsWithSdp()

        // 设置超时取消
        continuation.invokeOnCancellation {
            runCatching { appCtx.unregisterReceiver(receiver) }
        }

        // 超时处理
        if (timeout > Duration.ZERO) {
            continuation.context.job.invokeOnCompletion {
                if (!continuation.isActive) return@invokeOnCompletion
                appCtx.unregisterReceiver(receiver)
                continuation.resume(false)
            }
        }
    }
}