package com.munch1182.p1.receiver

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.munch1182.lib.base.Loglog
import com.munch1182.lib.base.getParcelableCompat
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.toHexStr
import com.munch1182.lib.helper.UsbDataHelper
import com.munch1182.lib.helper.UsbHelper
import com.munch1182.lib.helper.hasPermission
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.setContentWithTheme
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.TextSm
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter

class UsbActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dev = intent.getParcelableCompat(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        UserUsbHelper.getDevFromActivity(dev)
        setContentWithTheme { p ->
            UseView(Modifier.padding(p))
        }
    }
}

@Composable
private fun UseView(modifier: Modifier, vm: UsbViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PagePadding)
    ) {
        uiState.dev?.let {
            Items {
                Row {
                    Text(it.productName ?: "null")
                    Text(
                        it.deviceName, fontSize = TextSm, modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        it.hasPermission.toString(), fontSize = TextSm, modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text("${it.vendorId.toHexStr()}(${it.vendorId})")
                Text("${it.productId.toHexStr()}(${it.productId})")
            }
        } ?: Text("error")
        SpacerV()
        Text(uiState.state)
    }
}

@Stable
class UsbViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UsbUiState(null))
    val uiState = _uiState.asStateFlow()
    private var connector: UsbDataHelper? = null

    init {
        viewModelScope.launchIO {
            UserUsbHelper.curr.collect {
                _uiState.emit(UsbUiState(it?.dev))
                val dev = it?.dev
                if (dev != null) {
                    handDev(dev)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connector?.close()
    }

    private fun handDev(dev: UsbDevice) {
        if (dev.hasPermission) {
            viewModelScope.launchIO {
                _uiState.emit(_uiState.value.copy(dev, state = "开始连接"))
                val connect = UsbHelper.usbManager.openDevice(dev)
                if (connect != null) {
                    val intf = List(dev.interfaceCount) { dev.getInterface(it) }.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_HID }
                    if (intf != null) {
                        connect.claimInterface(intf, true)
                        _uiState.emit(_uiState.value.copy(dev, state = "连接成功"))
                        connector = UsbHelper.connect(dev, intf, connect)
                        connector?.receiveAsFlow()?.filter { it !is UsbDataHelper.ReceiveDataEvent.Error }?.collect {
                            Loglog.log(it)
                        }
                    }
                }
                _uiState.emit(_uiState.value.copy(dev, state = "连接识别"))
            }

        } else {
            val job = SupervisorJob()
            viewModelScope.launchIO(job) {
                UsbHelper.devState.collect {
                    if (it.state == UsbHelper.State.PermissionGranted && it.dev == dev) {
                        _uiState.emit(_uiState.value.copy(dev, state = "权限已授权"))
                        job.cancel()
                    }
                }
                _uiState.emit(_uiState.value.copy(dev, state = "开始请求权限"))
                UsbHelper.requestUsbPermission(dev)
            }

        }
    }
}

@Stable
data class UsbUiState(val dev: UsbDevice?, val state: String = "")