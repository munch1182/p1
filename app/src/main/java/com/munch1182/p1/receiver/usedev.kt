package com.munch1182.p1.receiver

import android.hardware.usb.UsbDevice
import androidx.compose.runtime.Stable
import com.munch1182.android.lib.AppHelper
import com.munch1182.android.lib.base.Loglog
import com.munch1182.android.lib.base.launchIO
import com.munch1182.android.lib.helper.UsbHelper
import com.munch1182.android.lib.helper.hasPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserUsbHelper {
    private var _curr = MutableStateFlow<UserUsbDev?>(null)
    val curr = _curr.asStateFlow()
    val currUsb get() = _curr.value

    init {
        AppHelper.launchIO {
            UsbHelper.devState.collect {
                Loglog.logStr("devState: $it")
                when (it.state) {
                    UsbHelper.State.Attached -> it.dev?.let { d -> init(d) }
                    UsbHelper.State.Detached -> {
                        _curr.value = null
                    }

                    UsbHelper.State.PermissionDenied -> (currUsb?.dev)?.let { d -> init(d) }
                    UsbHelper.State.PermissionGranted -> (currUsb?.dev)?.let { d -> init(d) }
                }
            }
        }
    }

    fun init(dev: UsbDevice) {
        _curr.value = UserUsbDev(dev)
    }

    fun getDevFromUsbActivity(dev: UsbDevice?) {
        if (currUsb == null) {
            if (dev != null) init(dev) else UsbHelper.getDevs()?.firstOrNull()?.let { init(it) }
        }
    }

    @Stable
    data class UserUsbDev(val dev: UsbDevice, val hasPermission: Boolean = dev.hasPermission) {

        override fun toString() = "UserUsbDev(dev=${dev.productName}, hasPermission=$hasPermission)"
    }
}