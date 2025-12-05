package com.munch1182.p1.views

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.munch1182.lib.base.appSetting
import com.munch1182.lib.base.isBluetoothOpen
import com.munch1182.lib.base.isGpsOpen
import com.munch1182.lib.helper.result.ifAllGranted
import com.munch1182.lib.helper.result.ifTrue
import com.munch1182.lib.helper.result.judge
import com.munch1182.lib.helper.result.permission
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.onDialog
import com.munch1182.p1.ui.ClickIcon
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.theme.PagePaddingModifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun BluetoothView(vm: BluetoothVM = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    var isOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateButton(if (uiState.isScanning) "停止扫描" else "开始扫描", uiState.isScanning) {
                withBlueScanPermission { vm.toggleScan() }
            }
            ClickIcon(Icons.AutoMirrored.Filled.ArrowLeft, modifier = Modifier.rotate(if (isOpen) 90f else 270f), onClick = {
                isOpen = !isOpen
                if (isOpen) showBlueFilterView()
            })
        }
    }
}

private fun showBlueFilterView() {
    DialogHelper.newBottom {
        var isChecked by remember { mutableStateOf(false) }
        Column {
            SettingItem("隐藏无名称蓝牙", isChecked) { isChecked = !isChecked }
            SettingItem("显示已连接设备", !isChecked) { isChecked = !isChecked }
        }
    }.show()
}

@Composable
private fun SettingItem(str: String, isCheck: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        PagePaddingModifier
            .fillMaxWidth()
            .clickable(true) {
                onCheckedChange(!isCheck)
            }, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(str)
        Spacer(Modifier.weight(1f))
        Switch(isCheck, onCheckedChange, Modifier.size(24.dp, 8.dp))
    }
}


@Stable
class BluetoothVM : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState = _uiState.asStateFlow()

    fun toggleScan() {
        if (_uiState.value.isScanning) stopScan() else startScan()
    }

    private fun startScan() {
        _uiState.value = _uiState.value.copy(isScanning = true)
    }

    private fun stopScan() {
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}

@Stable
data class BluetoothUiState(val isScanning: Boolean = false)

private fun withBlueScanPermission(any: () -> Unit) {
    val list = mutableListOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        list.add(Manifest.permission.BLUETOOTH_SCAN)
        list.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    permission(list).onDialog("蓝牙设备", "扫描附近蓝牙设备").onIntent(appSetting()).ifAllGranted()
        .judge({ isBluetoothOpen() }, Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)).onDialog("打开蓝牙", "请打开蓝牙开关").ifTrue()
        .request {
            if (it) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    judge({ isGpsOpen() }, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)).onDialog("位置开关", "定位权限用于扫描附近蓝牙设备，请打开", ok = "打开").ifTrue()
                        .permission(listOf(Manifest.permission.ACCESS_COARSE_LOCATION)).onDialog("位置权限", "扫描附近蓝牙设备").onIntent(appSetting()).ifAllGranted()
                        .request { loc -> if (loc) any() }
                } else {
                    any()
                }
            }
        }
}