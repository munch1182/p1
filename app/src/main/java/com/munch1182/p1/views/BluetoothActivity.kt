package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.launchDefault
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.blue.OnBluetoothStateChangeListener
import com.munch1182.lib.helper.blue.scan.BluetoothScanningListener
import com.munch1182.lib.helper.blue.scanResult
import com.munch1182.lib.helper.isLocationProvider
import com.munch1182.lib.helper.result.ifAllGranted
import com.munch1182.lib.helper.result.ifTrue
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.result.permissions
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.intentBlueScanDialog
import com.munch1182.p1.base.permissionBlueScanDialog
import com.munch1182.p1.base.permissionDialog
import com.munch1182.p1.base.toast
import com.munch1182.p1.ui.CheckBoxWithLabel
import com.munch1182.p1.ui.Rv
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.FontDescSize
import com.munch1182.p1.ui.theme.FontManySize
import com.munch1182.p1.ui.theme.FontTitleSize
import com.munch1182.p1.ui.theme.ItemPadding
import com.munch1182.p1.ui.theme.PagePaddingHalf
import com.munch1182.p1.ui.theme.PagePaddingModifier
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class BluetoothActivity : BaseActivity() {
    private val envVM by viewModels<BluetoothEnvVM>()
    private val scanVM by viewModels<BluetoothScanVM>()
    private val connectVM by viewModels<BluetoothConnectVM>()

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

    @Composable
    private fun Operate() {
        val isScanning by scanVM.scanning.observeAsState(false)
        val scanFilter by scanVM.scanFilter.observeAsState(BluetoothScanVM.ScanFilter())
        val isBlueOn by envVM.isBlueOn.observeAsState(false)
        var hideFilter by remember { mutableStateOf(true) }
        var rssi by remember { mutableFloatStateOf(100f) }

        if (!isBlueOn && isScanning) {
            scanVM.stopScan()
        }

        Column(PagePaddingModifier) {
            Text(scanFilter.toString(), modifier = Modifier.clickable { hideFilter = !hideFilter })
            if (!hideFilter) {
                CheckBoxWithLabel("包含没有名称的蓝牙", scanFilter.noNoName) { scanVM.updateScanFilter(scanFilter.newIgnoreName()) }
                CheckBoxWithLabel("包含已连接的蓝牙", scanFilter.isConnected) { scanVM.updateScanFilter(scanFilter.newConnected()) }
                CheckBoxWithLabel("包含已配对的蓝牙", scanFilter.isPaired) { scanVM.updateScanFilter(scanFilter.newPaired()) }
                CheckBoxWithLabel("经典蓝牙", scanFilter.isClassic) { scanVM.updateScanFilter(scanFilter.newClassic()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("信号强度：")
                    Slider(value = rssi, valueRange = 40f..100f, onValueChange = {
                        rssi = it
                        scanVM.updateScanFilter(scanFilter.newRssi(rssi.toInt()))
                    }, onValueChangeFinished = { })
                }
            }
            if (hideFilter) Split()
            StateButton(if (!isScanning) "扫描" else "停止扫描", isScanning) {
                withScanPermission { scanVM.toggleScan() }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun Devs() {
        val devs by scanVM.devices.observeAsState(arrayOf())
        Rv(devs, key = { it.address }) { blue ->
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(blue.canConnect) {
                        withConnectPermission {
                            scanVM.stopScan()
                            connectVM.connect(blue.dev)
                        }
                    }, verticalAlignment = Alignment.Bottom
            ) {
                Column(ItemPadding) {
                    Row {
                        Text(blue.name ?: "N/A", color = Color.Blue, fontSize = FontTitleSize, fontWeight = FontWeight.Bold)
                        if (blue.isConnected || blue.isPaired) Text(if (blue.isConnected) "connected" else "bonded", fontSize = FontDescSize, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = PagePaddingHalf))
                    }
                    Text("(${blue.address})", color = Color.Gray, fontSize = FontDescSize)
                }
                Spacer(Modifier.weight(1f))
                blue.rssiStr?.let { r ->
                    Column(ItemPadding) {
                        Text(r, color = Color.Gray, fontSize = FontManySize)
                    }
                }
            }
        }
    }

    private fun withConnectPermission(canConnect: () -> Unit) {
        val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH)
        }
        permissions(p)
            .permissionDialog("蓝牙", "连接蓝牙")
            .manualIntent()
            .ifAllGranted()
            .judge { BluetoothHelper.isBlueOn }
            .intent(BluetoothHelper.enableBlueIntent())
            .ifTrue()
            .requestAll { if (it) canConnect() else toast("没有权限去连接蓝牙！") }
    }

    private fun withScanPermission(canScan: () -> Unit) {
        val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH)
        }
        permissions(p)
            .permissionDialog("蓝牙", "扫描蓝牙")
            .manualIntent()
            .ifAllGranted()
            .judge { BluetoothHelper.isBlueOn }
            .intent(BluetoothHelper.enableBlueIntent())
            .ifTrue()
            .judge { isLocationProvider }
            .intent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            .intentBlueScanDialog()
            .ifTrue()
            .permission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) // 定位打开此权限才会判断为true
            .permissionBlueScanDialog()
            .ifAllGranted()
            .requestAll { if (it) canScan() else toast("没有权限去扫描蓝牙！") }
    }
}

class BluetoothEnvVM : ViewModel() {
    private val log = log()

    private val _isBlueOn = MutableLiveData(false)

    val isBlueOn = _isBlueOn.asLive()

    private val state = OnBluetoothStateChangeListener { curr, prev ->
        log.logStr("state: $curr => $prev")
        _isBlueOn.postValue(curr?.isOn ?: BluetoothHelper.isBlueOn)
    }

    init {
        _isBlueOn.postValue(BluetoothHelper.isBlueOn)
        BluetoothHelper.addBluetoothStateListener(state)
    }

    override fun onCleared() {
        super.onCleared()
        BluetoothHelper.removeBluetoothStateListener(state)
    }
}

class BluetoothScanVM : ViewModel() {
    private val log = log()
    private var _scanning = MutableLiveData(false)
    private var _devs = MutableLiveData<Array<BlueDev>>(arrayOf())
    private var _filter = MutableLiveData(ScanFilter())

    val scanning = _scanning.asLive()
    val devices = _devs.asLive()
    val scanFilter = _filter.asLive()

    private val scanningListener = BluetoothScanningListener { _scanning.postValue(it) }
    private val devs = LinkedHashMap<String, BlueDev>()
    private val runner = Channel<Array<BlueDev>>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        BluetoothHelper.CLASSIC.addScanningListener(scanningListener)
        BluetoothHelper.addScanningListener(scanningListener)
        viewModelScope.launchDefault {
            log.logStr("runner start loop")
            while (coroutineContext.isActive) {
                val receive = runner.receive()
                _devs.postValue(receive)
            }
            log.logStr("runner end loop")
        }
    }

    @SuppressLint("MissingPermission")
    fun toggleScan() {
        log.logStr("toggleScan")
        if (scanning.value == true) {
            stopScan()
        } else {
            _scanning.postValue(true)
            devs.clear()
            _devs.postValue(arrayOf())
            viewModelScope.launchDefault {
                _filter.value!!.startScan {
                    devs[it.address] = it
                    runBlocking { runner.send(devs.values.toTypedArray()) }
                }
            }
        }
    }

    fun stopScan() {
        if (_scanning.value != true) return
        _filter.value?.stopScan()
    }

    override fun onCleared() {
        super.onCleared()
        BluetoothHelper.removeScanningListener(scanningListener)
        BluetoothHelper.CLASSIC.removeScanningListener(scanningListener)
    }

    fun updateScanFilter(filter: ScanFilter) {
        if (scanning.value == true) {
            _filter.value!!.stopScan()
        }
        log.logStr("updateScanFilter: $filter => ${_filter.value}")
        _filter.postValue(filter)
    }

    @SuppressLint("MissingPermission")
    data class BlueDev(val dev: BluetoothDevice, val rssi: Int, val from: From = From.BLEScan) {
        val name: String? get() = dev.name
        val address: String get() = dev.address
        val rssiStr: String? get() = if (from.isBle) "$rssi dBm" else null

        val canConnect get() = dev.isBle
        val isConnected get() = from.isConnected
        val isPaired get() = from.isPaired

        companion object {
            fun from(dev: BluetoothDevice, rssi: Int = 0) = BlueDev(dev, rssi, From.ClassicScan)
            fun from(scan: ScanResult) = BlueDev(scan.device, scan.rssi)
            fun fromConnect(dev: BluetoothDevice, rssi: Int = 0) = BlueDev(dev, rssi, From.Connected)
            fun fromPair(dev: BluetoothDevice, rssi: Int = 0) = BlueDev(dev, rssi, From.Paired)

            private val BluetoothDevice.isBle get() = type == BluetoothDevice.DEVICE_TYPE_LE || type == BluetoothDevice.DEVICE_TYPE_DUAL
        }
    }

    sealed class From {
        data object ClassicScan : From()
        data object BLEScan : From()
        data object Connected : From()
        data object Paired : From()

        val isBle get() = this is BLEScan
        val isConnected get() = this is Connected
        val isPaired get() = this is Paired
    }

    @SuppressLint("MissingPermission")
    data class ScanFilter(val noNoName: Boolean = false, val isConnected: Boolean = true, val isPaired: Boolean = false, val find: String? = null, val minSSID: Int = 100, val isClassic: Boolean = false) {

        fun newIgnoreName(noNoName: Boolean = !this.noNoName) = this.copy(noNoName = noNoName)
        fun newClassic(isClassic: Boolean = !this.isClassic) = this.copy(isClassic = isClassic)
        fun newConnected(isConnected: Boolean = !this.isConnected) = this.copy(isConnected = isConnected)
        fun newRssi(minSSID: Int = 100) = this.copy(minSSID = minSSID)
        fun newPaired(paired: Boolean = !isPaired) = this.copy(isPaired = paired)

        private fun filter(blueDev: BlueDev, ignore: Boolean = false): BlueDev? {
            if (ignore) return blueDev
            val dev = blueDev.dev
            if (!noNoName && dev.name == null) return null
            if (find != null && dev.name?.contains(find) != true) return null
            if (!isClassic && minSSID != 100 && minSSID.absoluteValue < blueDev.rssi.absoluteValue) return null
            return blueDev
        }

        fun stopScan() {
            if (isClassic) {
                BluetoothHelper.CLASSIC.stopScan()
            } else {
                BluetoothHelper.stopScan()
            }
        }

        suspend fun startScan(l: OnResultListener<BlueDev>) {
            var connected: List<BlueDev> = emptyList()
            if (isConnected) {
                connected = suspendCoroutine { c -> BluetoothHelper.allConnectedDevs { c.resume(it.mapNotNull { dev -> filter(BlueDev.fromConnect(dev)) }) } }
                connected.forEach { l.onResult(it) }
            }
            if (isPaired) {
                BluetoothHelper.bondDevs?.filter {
                    connected.find { f -> f.address == it.address }?.let { false } ?: true
                }?.forEach { dev ->
                    filter(BlueDev.fromPair(dev))?.let { dev2 -> l.onResult(dev2) }
                }
            }
            if (isClassic) {
                BluetoothHelper.CLASSIC.apply { setScannedListener { dev -> filter(BlueDev.from(dev))?.let { dev2 -> l.onResult(dev2) } } }.startScan()
            } else {
                BluetoothHelper.scanResult { dev -> filter(BlueDev.from(dev))?.let { dev2 -> l.onResult(dev2) } }
            }
        }

        override fun toString(): String {

            if (find != null) {
                return "查找${find}"
            } else {
                val list = mutableListOf<String>()
                if (noNoName) list.add("包含无名字的") else list.add("有名字的")
                if (isConnected) list.add("包含已连接的")
                if (isPaired) list.add("包含已配对的")
                if (minSSID < 100) list.add("信号强度小于-${minSSID}的")

                val sb = StringBuilder()
                list.forEachIndexed { index, s ->
                    if (index == 0) sb.append(s) else sb.append("、$s")
                }

                if (isClassic) sb.append("经典蓝牙") else sb.append("低功耗蓝牙")
                return sb.toString()
            }
        }
    }
}

@SuppressLint("MissingPermission")
class BluetoothConnectVM : ViewModel() {
    private val log = log()
    fun connect(dev: BluetoothDevice) {
        val mac = dev.address
        log.logStr("connect: $mac")
        //BluetoothHelper.startConnect(mac)
    }
}