package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.base.ReRunJob
import com.munch1182.lib.base.blueSetting
import com.munch1182.lib.base.copyText
import com.munch1182.lib.base.isBluetoothOpen
import com.munch1182.lib.base.isGpsOpen
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.locSetting
import com.munch1182.lib.base.toHexStr
import com.munch1182.lib.base.toast
import com.munch1182.lib.bluetooth.le.BLEConnector
import com.munch1182.lib.bluetooth.le.BleConnectManager
import com.munch1182.lib.bluetooth.le.BlueScanRecordHelper
import com.munch1182.lib.bluetooth.le.CommandResult
import com.munch1182.lib.bluetooth.le.leScanFlow
import com.munch1182.lib.helper.onResult
import com.munch1182.lib.helper.result.ifAll
import com.munch1182.lib.helper.result.isOk
import com.munch1182.lib.helper.result.manualIntent
import com.munch1182.lib.helper.result.permissions
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.BleSender
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.SnQuery
import com.munch1182.p1.base.initBlue
import com.munch1182.p1.base.onIntent
import com.munch1182.p1.base.onPermission
import com.munch1182.p1.ui.CheckBoxLabel
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.ClickIcon
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.RvPage
import com.munch1182.p1.ui.ScrollPage
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.paddingNoBottom
import com.munch1182.p1.ui.paddingNoTop
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingHalf
import com.munch1182.p1.ui.theme.PagePaddingModifier
import com.munch1182.p1.ui.theme.TextLg
import com.munch1182.p1.ui.theme.TextSm
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update

class BluetoothActivity : BaseActivity() {

    private val vm by viewModels<BluetoothVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll { Views() }
    }

    @Composable
    private fun Views() {
        val uiState by vm.uiState.collectAsState()
        var isExpand by remember { mutableStateOf(false) }
        val target by remember { derivedStateOf { !uiState.filter.target.isNullOrBlank() } }

        Row(verticalAlignment = Alignment.CenterVertically) {
            StateButton(if (uiState.isScanning) "停止扫描" else "开始扫描", uiState.isScanning) {
                withPermission { if (uiState.isScanning) vm.stopScan() else vm.startScan() }
            }
            ClickIcon(Icons.Filled.ArrowDropDown, modifier = Modifier.rotate(if (isExpand) 0f else 180f)) {
                isExpand = !isExpand
                if (isExpand) showScanFilterDialog { isExpand = false }
            }
            if (target) Text(uiState.filter.target ?: "")
        }

        BlueDevListView(devices = uiState.devices.toTypedArray(), modifier = Modifier.fillMaxWidth(), onClick = {
            vm.stopScan()
            showConnectDialog(it)
        }, onLongClick = {
            vm.stopScan()
            showRecordDialog(it)
        })
    }

    private fun showScanFilterDialog(onResult: (Boolean) -> Unit) {
        vm.stopScan()
        DialogHelper.newBottom {
            val state by vm.uiState.collectAsState()
            Items(PagePaddingModifier) {
                CheckBoxLabel("忽略没有名称的设备", state.filter.ignoreNoName) {
                    vm.updateFilter(state.filter.copy(ignoreNoName = it))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("包含：")
                    TextField(state.filter.target ?: "", { vm.updateFilter(state.filter.copy(target = it)) }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(100.dp))
            }
        }.onResult(onResult).show()
    }

    private fun showRecordDialog(dev: BlueDev) {
        val dev = dev as? BlueDev.Scan ?: return
        val devs = BlueScanRecordHelper.parseScanRecord(dev.scanResult.scanRecord?.bytes ?: ByteArray(0)).toTypedArray()
        DialogHelper.newBottom {
            var showType by remember { mutableIntStateOf(0) }
            Column(modifier = Modifier.sizeIn(minHeight = 250.dp)) {
                ClickIcon(Icons.Filled.Autorenew) { showType = (showType + 1) % 2 }
                when (showType) {
                    0 -> RvPage(
                        devs, modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Transparent)
                            .padding(vertical = PagePadding)
                    ) {
                        Text(text = it.typeStr(), modifier = Modifier.paddingNoBottom(PagePadding, PagePaddingHalf), fontSize = TextSm)
                        Text(text = it.value2StrIfTypeCan(), modifier = Modifier.paddingNoTop(PagePadding, PagePaddingHalf))
                    }

                    1 -> Text(dev.scanResult.scanRecord?.bytes?.toHexStr() ?: "", PagePaddingModifier)
                    else -> {}
                }
            }
        }.show()
    }

    private fun withPermission(any: suspend () -> Unit) {
        val permission = mutableListOf(
            Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permission.addAll(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
        lifecycleScope.launchIO {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions(permission.toTypedArray()).onPermission("蓝牙" to "蓝牙相关功能").manualIntent().ifAll().judge({ isBluetoothOpen() }, blueSetting()).onIntent("请前往蓝牙界面打开蓝牙，以使用蓝牙功能").request { if (it) lifecycleScope.launchIO { any() } }
            } else {
                permissions(permission.toTypedArray()).onPermission("蓝牙" to "蓝牙相关功能").manualIntent().ifAll().judge({ isGpsOpen() }, locSetting()).onIntent("请前往位置界面打开定位，以扫描附近蓝牙设备").isOk().permission(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ).onPermission("定位" to "扫描附近蓝牙设备").manualIntent().ifAll().judge({ isBluetoothOpen() }, blueSetting()).onIntent("请前往蓝牙界面打开蓝牙，以使用蓝牙功能").request { if (it) lifecycleScope.launchIO { any() } }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun showConnectDialog(dev: BlueDev) {
        DialogHelper.newBottom {
            val uiState by vm.uiState.collectAsState()
            ScrollPage(PagePaddingModifier) {
                StateButton(if (uiState.connectState.isCanStart) "开始连接" else "断开连接", !uiState.connectState.isCanStart) {
                    withPermission { if (uiState.connectState.isCanStart) vm.connect(dev) else vm.disconnect(dev) }
                }
                Items(
                    modifier = Modifier
                        .padding(vertical = PagePadding)
                        .height(350.dp)
                ) {
                    Text("${dev.name}(${dev.mac})", fontSize = TextLg, fontWeight = FontWeight.Bold)
                    Text("${uiState.connectState}", fontSize = TextSm, fontWeight = FontWeight.Bold)
                    if (uiState.connectState.isConnected) {
                        ClickButton("获取SN码", modifier = Modifier.padding(vertical = PagePadding)) { vm.sendSn(dev) }
                        val sendResult = uiState.sendResult as? CommandResult<String>
                        if (sendResult != null) {
                            if (sendResult.isSuccess) {
                                val result: String = sendResult.getOrNull() ?: ""
                                Text(result, fontSize = TextLg, modifier = Modifier.clickable(true) {
                                    copyText(result)
                                    toast("已复制到剪贴板")
                                })
                            } else {
                                Text(uiState.sendResult.toString())
                            }
                        }
                    }
                }
            }
        }.onResult { vm.disconnect(dev) }.show()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlueDevListView(devices: Array<BlueDev>, modifier: Modifier = Modifier, onLongClick: ((BlueDev) -> Unit)? = null, onClick: (BlueDev) -> Unit = {}) {
    RvPage(devices, modifier = modifier, key = { it.mac }) { device ->
        BlueDevItemView(device = device, modifier = Modifier.combinedClickable(enabled = true, onClick = { onClick(device) }, onLongClick = { onLongClick?.invoke(device) }))
    }
}

@Composable
private fun BlueDevItemView(device: BlueDev, modifier: Modifier = Modifier) {
    Card(modifier = Modifier.padding(vertical = PagePaddingHalf)) {
        Column(modifier = modifier.padding(PagePadding)) {
            Text(device.name, Modifier.align(Alignment.Start), fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(device.mac, fontSize = TextSm)
                Text(device.state, fontSize = TextSm)
            }
        }
    }
}

class BluetoothVM : ViewModel() {
    private val devs = LinkedHashMap<String, BlueDev>()
    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState = _uiState.asStateFlow()

    private val scanJob = ReRunJob()

    @OptIn(FlowPreview::class)
    fun startScan() {
        _uiState.update { it.copy(isScanning = true, devices = emptyList()) }
        viewModelScope.launchIO(scanJob.newContext) {
            leScanFlow().filter(_uiState.value.predicate).onEach { devs[it.device.address] = BlueDev.Scan(it) }.sample(650L).map { devs.values.toList() }.collect {
                _uiState.emit(_uiState.value.copy(devices = it))
            }
        }
    }

    fun stopScan() {
        _uiState.update { it.copy(isScanning = false) }
        scanJob.cancel()
    }

    fun updateFilter(filter: Filter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun connect(dev: BlueDev) {
        _uiState.update { it.copy(connectState = BleConnectState.IsConnecting) }
        viewModelScope.launchIO {
            BleConnectManager.connect(dev.dev, viewModelScope).collect { state ->
                if (state.isConnected) {
                    val connector = BleConnectManager.getConnector(dev.mac) ?: return@collect
                    val init = connector.initBlue()
                    val newState = if (init.isSuccess) BLEConnector.ConnectState.Connected else BLEConnector.ConnectState.Disconnected
                    if (newState.isConnected) {
                        _uiState.update { it.copy(connectState = BleConnectState.ConnectedSuccess) }
                    } else {
                        disconnect(dev)
                        _uiState.update { it.copy(connectState = BleConnectState.ConnectedFail(init.toString())) }
                    }
                } else if ((state.isDisconnected || state.isDisconnecting) && (_uiState.value.connectState !is BleConnectState.ConnectedFail)) {
                    _uiState.update { it.copy(connectState = BleConnectState.Disconnected) }
                    BleSender.unregister(dev.mac)
                } else if (state.isConnecting) {
                    _uiState.update { it.copy(connectState = BleConnectState.IsConnecting) }
                }
            }
        }
    }

    fun disconnect(dev: BlueDev) {
        _uiState.update {
            it.copy(connectState = BleConnectState.Disconnected, sendResult = null)
        }
        BleConnectManager.disconnect(dev.mac)
    }

    override fun onCleared() {
        super.onCleared()
        BleConnectManager.cleanup()
    }

    fun sendSn(dev: BlueDev) {
        viewModelScope.launchIO {
            val result = BleSender.sendCommand(SnQuery(dev.mac))
            _uiState.update { it.copy(sendResult = result) }
        }
    }
}

data class Filter(
    val ignoreNoName: Boolean = true,
    val target: String? = null,
)

data class BluetoothUiState(
    val isScanning: Boolean = false, val connectState: BleConnectState = BleConnectState.Disconnected, val filter: Filter = Filter(), val devices: List<BlueDev> = emptyList(), val sendResult: CommandResult<*>? = null
) {
    val predicate: suspend (ScanResult) -> Boolean
        @SuppressLint("MissingPermission") get() = res@{
            if (filter.ignoreNoName && it.device.name.isNullOrBlank()) return@res false
            val target = filter.target?.takeIf { t -> t.isNotBlank() }?.lowercase() ?: return@res true
            if ((it.device.name?.lowercase()?.contains(target)) == true) return@res true
            if (it.device.address.lowercase().contains(target)) return@res true
            return@res false
        }
}

sealed class BleConnectState {
    object IsConnecting : BleConnectState()
    object ConnectedSuccess : BleConnectState()
    class ConnectedFail(val msg: String) : BleConnectState()
    object Disconnected : BleConnectState()

    val isCanStart get() = this is Disconnected || this is ConnectedFail
    val isConnected get() = this is ConnectedSuccess
    val isConnecting get() = this is IsConnecting


    override fun toString(): String {
        return when (this) {
            is IsConnecting -> "连接中"
            is ConnectedSuccess -> "已连接"
            is ConnectedFail -> "连接失败: $msg"
            is Disconnected -> "未连接"
        }
    }
}

@SuppressLint("MissingPermission")
sealed class BlueDev(val mac: String) {

    val state
        get() = when (this) {
            is Bind -> ""
            is Scan -> "${scanResult.rssi} dBm"
        }

    val name
        get() = when (this) {
            is Bind -> dev.name
            is Scan -> scanResult.device.name
        } ?: "Unknown Device"

    val dev: BluetoothDevice
        get() = when (this) {
            is Bind -> this._dev
            is Scan -> this.scanResult.device
        }

    class Scan(val scanResult: ScanResult) : BlueDev(scanResult.device.address)

    class Bind(internal val _dev: BluetoothDevice) : BlueDev(_dev.address)
}