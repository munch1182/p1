package com.munch1182.p1.receiver

import android.hardware.usb.UsbDevice
import androidx.compose.runtime.Stable
import com.munch1182.android.lib.helper.UsbHelper
import com.munch1182.android.lib.helper.hasPermission
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserUsbHelper {
    const val VENDOR_ID = 0x35BB
    const val PRODUCT_ID = 0x1600
    private var _curr = MutableStateFlow<UserUsbDev?>(null)
    val curr = _curr.asStateFlow()
    val currUsb = _curr.value

    init {
        AppHelper.launchIO {
            UsbHelper.devState.collect {
                when (it.state) {
                    UsbHelper.State.Attached -> init(it.dev)
                    UsbHelper.State.Detached -> {
                        _curr.value = null
                    }

                    UsbHelper.State.PermissionDenied -> init(it.dev)
                    UsbHelper.State.PermissionGranted -> init(it.dev)
                }
            }
        }
    }

    fun init(dev: UsbDevice) {
        _curr.value = UserUsbDev(dev)
    }

    fun getDevFromActivity(dev: UsbDevice?) {
        if (currUsb == null && dev != null) init(dev)
    }

    @Stable
    data class UserUsbDev(val dev: UsbDevice, val hasPermission: Boolean = dev.hasPermission)
}