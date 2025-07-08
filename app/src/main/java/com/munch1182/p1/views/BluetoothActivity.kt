package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.base.toHexStr
import com.munch1182.lib.base.toHexStrNo0xNoSep
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.blue.connect
import com.munch1182.lib.helper.blue.connect.LeConnector
import com.munch1182.lib.helper.blue.scan.BlueScanRecordHelper
import com.munch1182.lib.helper.blue.scanResult
import com.munch1182.lib.helper.dialog.onResult
import com.munch1182.lib.helper.isLocationProvider
import com.munch1182.lib.helper.result.ifAllGranted
import com.munch1182.lib.helper.result.ifTrue
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.result.permissions
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.intentBlueScanDialog
import com.munch1182.p1.base.permissionBlueScanDialog
import com.munch1182.p1.base.permissionDialog
import com.munch1182.p1.base.toast
import com.munch1182.p1.ui.CheckBoxWithLabel
import com.munch1182.p1.ui.ComposeView
import com.munch1182.p1.ui.Rv
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.FontManySize
import com.munch1182.p1.ui.theme.FontTitleSize
import com.munch1182.p1.ui.theme.ItemPadding
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BluetoothActivity : BaseActivity() {

    private val log = log()
    private val ble by viewModels<BleVM>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll(Modifier) { Views() }
    }

    @Composable
    private fun Views() {
        Operate()
        Split()
        Devs()
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun Operate() {
        val isScanning by ble.isScanning.observeAsState(false)
        val isIgnoreName by ble.ignoreNoName.observeAsState(false)
        Row(modifier = PagePaddingModifier) {
            StateButton(if (isScanning) "停止扫描" else "开始扫描", isScanning) { withScanPermission { ble.toggleScan() } }

            CheckBoxWithLabel("忽略没有名称的蓝牙", isIgnoreName) { ble.toggleIgnoreName() }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun Devs() {
        val devs by ble.devs.collectAsState()
        Rv(devs, key = { it.mac }) { Item(it) }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @SuppressLint("MissingPermission")
    @Composable
    private fun Item(it: BleDev) {
        var isShow by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .combinedClickable(enabled = it.hadScanRecord, onClick = {
                        isShow = !isShow
                        withScanPermission { ble.stopScan() }
                    }, onLongClick = {
                        withScanPermission { ble.stopScan() }
                        withConnectPermission { showConnectView(it) }
                    })
                    .padding(16.dp, 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(it.name ?: "N/A", fontWeight = FontWeight.Bold, color = Color.Blue)
                    Text(it.mac, color = Color.Gray, fontWeight = FontWeight.W400, fontSize = FontManySize)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    val stateStr = it.stateStr()
                    if (stateStr != null) Text(stateStr, fontWeight = FontWeight.Bold, fontSize = FontManySize)
                    if (it is BleDev.Scanned) Text("${it.rssi} dBm", fontWeight = FontWeight.W400, fontSize = FontManySize)
                }
            }
            if (isShow) {
                val scanRecord = (it as? BleDev.Scanned)?.scanRecord
                if (scanRecord == null) {
                    Text("没有广播数据", modifier = PagePaddingModifier)
                } else {
                    Column(
                        modifier = Modifier
                            .clickable { showRecordDialog(scanRecord) }
                            .padding(PagePadding)) {
                        Text("广播数据：", fontWeight = FontWeight.W400, fontSize = FontManySize, color = Color.Black)
                        Text(scanRecord.bytes.joinToString(separator = "") { String.format("%02X", it) }, fontWeight = FontWeight.W400, fontSize = FontManySize, color = Color.Black)
                    }
                }
            }
        }
    }

    private fun showRecordDialog(record: ScanRecord) {
        DialogHelper.newBottom { it, _ ->
            ComposeView(it) {
                val records = BlueScanRecordHelper.parseScanRecord(record.bytes).toTypedArray()
                Rv(records, key = { it.type }, modifier = Modifier.padding(bottom = 16.dp, top = 16.dp)) {
                    Text("${it.typeStr()}(${it.type.toHexStr(true)})", modifier = ItemPadding, fontWeight = FontWeight.Bold)
                    Text(it.value.toHexStrNo0xNoSep(), modifier = ItemPadding)
                    when (it) {
                        is BlueScanRecordHelper.BlueRecord.Flags -> Text("${it.flagStr}(${it.typeStr})", modifier = ItemPadding)
                        is BlueScanRecordHelper.BlueRecord.LocalName -> Text(it.name, modifier = ItemPadding)
                        else -> {}
                    }
                }
            }
        }.show()
    }

    private fun BleDev.stateStr() = when (this) {
        is BleDev.Bond -> "BOND"
        is BleDev.Connected -> "CONNECTED"
        is BleDev.Scanned -> if (isBond) "BOND" else if (isConnect) "CONNECTED" else null
    }

    private fun withConnectPermission(canConnect: () -> Unit) {
        val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH)
        }
        permissions(p).permissionDialog("蓝牙", "连接蓝牙").manualIntent().ifAllGranted().judge { BluetoothHelper.isBlueOn }.intent(BluetoothHelper.enableBlueIntent()).ifTrue().requestAll { if (it) canConnect() else toast("没有权限去连接蓝牙！") }
    }

    private fun withScanPermission(canScan: () -> Unit) {
        val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH)
        }
        permissions(p).permissionDialog("蓝牙", "扫描蓝牙").manualIntent().ifAllGranted().judge { BluetoothHelper.isBlueOn }.intent(BluetoothHelper.enableBlueIntent()).ifTrue().judge { isLocationProvider }.intent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)).intentBlueScanDialog().ifTrue()
            .permission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) // 定位打开此权限才会判断为true
            .permissionBlueScanDialog().ifAllGranted().requestAll { if (it) canScan() else toast("没有权限去扫描蓝牙！") }
    }

    @SuppressLint("MissingPermission")
    private fun showConnectView(dev: BleDev) {
        lifecycleScope.launch(Dispatchers.Main) {
            DialogHelper.newBottom {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = PagePadding), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(dev.name, modifier = Modifier, fontWeight = FontWeight.Bold, fontSize = FontTitleSize)
                    Text(dev.mac, modifier = Modifier, fontSize = FontManySize)
                    BluetoothHelper.connect(dev.mac) {
                        BluetoothHelper.gattOps(
                            it, arrayOf(LeConnector.GattOp.FindServices { l ->
                                l?.forEach { a -> log.logStr("服务：${a.uuid}") }
                                true
                            })
                        )
                    }
                }
            }.onResult {

            }.show()
        }
    }
}

sealed class BleDev(val dev: BluetoothDevice) {
    class Connected(dev: BluetoothDevice) : BleDev(dev)
    class Bond(dev: BluetoothDevice) : BleDev(dev)
    class Scanned(
        dev: BluetoothDevice, val rssi: Int, val scanRecord: ScanRecord? = null, var isBond: Boolean = false, // 已绑定未连接的设备也可能被扫到
        var isConnect: Boolean = false // 对应双模蓝牙经典蓝牙已连接但是ble广播仍在广播的情形
    ) : BleDev(dev)

    val name @SuppressLint("MissingPermission") get() = dev.name
    val mac get() = dev.address
    val hadScanRecord get() = this is Scanned && scanRecord != null

    fun update(dev: BleDev?): BleDev? {
        dev ?: return null
        if (this is Bond && dev is Scanned) {
            return dev.apply { isBond = true }
        }
        if (this is Connected) {
            if (dev is Bond) return this
            if (dev is Scanned) return dev.apply { isConnect = true }
        }
        if (this is Scanned && dev is Scanned) {
            return dev.apply { isBond = this@BleDev.isBond; isConnect = this@BleDev.isConnect }
        }
        return dev
    }

    companion object {
        fun newConnected(dev: BluetoothDevice): BleDev {
            return Connected(dev)
        }

        fun newBond(dev: BluetoothDevice): BleDev {
            return Bond(dev)
        }

        fun newScanned(dev: ScanResult): BleDev {
            return Scanned(dev.device, dev.rssi, dev.scanRecord)
        }
    }
}

class BleVM : ViewModel() {
    private val log = log()
    private val _isScanning = MutableLiveData(false)
    private val _devs = linkedMapOf<String, BleDev>()
    private val _devsArr = MutableStateFlow(arrayOf<BleDev>())
    private val _ignoreNoName = MutableLiveData(true)

    val isScanning = _isScanning.asLive()
    val devs = _devsArr.asStateFlow()
    val ignoreNoName = _ignoreNoName.asLive()

    init {
        BluetoothHelper.addScanningListener {
            _isScanning.postValue(it)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        kotlin.runCatching { BluetoothHelper.stopScan() }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun toggleScan() {
        if (_isScanning.value == true) {
            BluetoothHelper.stopScan()
        } else {
            _devs.clear()
            viewModelScope.launchIO {
                _devsArr.emit(arrayOf())
                val connected = suspendCancellableCoroutine { c -> BluetoothHelper.allConnectedDevs { c.resume(it) } }
                connected.forEach { dev -> newScannedDev(BleDev.newConnected(dev)) }
                BluetoothHelper.bondDevs?.forEach { newScannedDev(BleDev.newBond(it)) }
                BluetoothHelper.scanResult { newScannedDev(BleDev.newScanned(it)) }
            }
            viewModelScope.launchIO {
                delay(350L)
                while (_isScanning.value == true) {
                    val ignore = _ignoreNoName.value == true
                    val toList = _devs.values.toList()
                    val newList = toList.filter { if (ignore) it.name != null else true }.toTypedArray()
                    _devsArr.emit(newList)
                    delay(100L * if (ignore) 1 else 5)
                }
            }
        }
    }

    private fun newScannedDev(dev: BleDev) {
        _devs[dev.mac] = _devs[dev.mac]?.update(dev) ?: dev
    }

    fun toggleIgnoreName() {
        _ignoreNoName.value = !_ignoreNoName.value!!
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        if (_isScanning.value == true) toggleScan()
    }

    fun connect(dev: BleDev) {
        log.logStr("connect ${dev.mac}")
    }
}