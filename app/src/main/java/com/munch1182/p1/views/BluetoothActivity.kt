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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.launchIO
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
import com.munch1182.p1.base.DialogHelper.intentBlueScan
import com.munch1182.p1.base.DialogHelper.permissionBlueScan
import com.munch1182.p1.base.DialogHelper.permissionDialog
import com.munch1182.p1.base.toast
import com.munch1182.p1.ui.CheckBoxWithLabel
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Rv
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.FontManySize
import com.munch1182.p1.ui.theme.FontTitleSize
import com.munch1182.p1.ui.theme.ItemPadding
import com.munch1182.p1.ui.theme.PagePaddingModifier
import kotlinx.coroutines.isActive
import java.util.concurrent.LinkedBlockingQueue
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

        if (!isBlueOn && isScanning) {
            scanVM.stopScan()
        }

        Column(PagePaddingModifier) {
            CheckBoxWithLabel("经典蓝牙", scanFilter.isClassic) { scanVM.updateScanFilter(scanFilter.newClassic()) }
            CheckBoxWithLabel("忽略没有名称的蓝牙", scanFilter.noNoName) { scanVM.updateScanFilter(scanFilter.newIgnoreName()) }
            ClickButton(if (!isScanning) "扫描" else "停止扫描") {
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
                    Text(blue.name ?: "N/A", color = Color.Blue, fontSize = FontTitleSize, fontWeight = FontWeight.Bold)
                    Text("(${blue.address})", color = Color.Gray, fontSize = FontManySize)
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
        permissions(p).dialogWhen(permissionDialog("蓝牙", "连接蓝牙")).manualIntent().ifAllGranted().judge { BluetoothHelper.isBlueOn }.intent(BluetoothHelper.enableBlueIntent()).ifTrue().requestAll { if (it) canConnect() else toast("没有权限去连接蓝牙！") }
    }

    private fun withScanPermission(canScan: () -> Unit) {
        val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH)
        }
        permissions(p).dialogWhen(permissionDialog("蓝牙", "扫描蓝牙")).manualIntent().ifAllGranted().judge { BluetoothHelper.isBlueOn }.intent(BluetoothHelper.enableBlueIntent()).ifTrue().judge { isLocationProvider }.intent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            .dialogWhen(intentBlueScan()).ifTrue().permission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) // 定位打开此权限才会判断为true
            .dialogWhen(permissionBlueScan()).ifAllGranted().requestAll { if (it) canScan() else toast("没有权限去扫描蓝牙！") }
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
    private val runner = LinkedBlockingQueue<Runnable>()

    init {
        BluetoothHelper.CLASSIC.addScanningListener(scanningListener)
        BluetoothHelper.addScanningListener(scanningListener)
        viewModelScope.launchIO {
            log.logStr("runner start loop")
            while (coroutineContext.isActive) {
                runner.take().run()
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
            viewModelScope.launchIO {
                _filter.value!!.startScan {
                    runner.put {
                        devs[it.address] = it
                        _devs.postValue(devs.values.toTypedArray())
                    }
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
    data class BlueDev(val dev: BluetoothDevice, val rssi: Int, val from: From = From.BLE) {
        val name: String? get() = dev.name
        val address: String get() = dev.address
        val rssiStr: String? get() = if (from.isBle) "$rssi dBm" else null

        val canConnect get() = dev.isBle

        companion object {
            fun from(dev: BluetoothDevice, rssi: Int = 0) = BlueDev(dev, rssi, From.Classic)
            fun from(scan: ScanResult) = BlueDev(scan.device, scan.rssi)
            fun fromSys(dev: BluetoothDevice, rssi: Int = 0) = BlueDev(dev, rssi, From.Sys)

            private val BluetoothDevice.isBle get() = type == BluetoothDevice.DEVICE_TYPE_LE || type == BluetoothDevice.DEVICE_TYPE_DUAL
        }
    }

    sealed class From {
        data object Classic : From()
        data object BLE : From()
        data object Sys : From()

        val isBle get() = this is BLE
    }

    @SuppressLint("MissingPermission")
    data class ScanFilter(val noNoName: Boolean = true, val find: String? = null, val minSSID: Int = 0, val isClassic: Boolean = false) {

        fun newIgnoreName() = ScanFilter(noNoName = !noNoName, find = find, minSSID = minSSID)
        fun newClassic() = ScanFilter(noNoName = noNoName, find = find, minSSID = minSSID, isClassic = !isClassic)

        private fun filter(blueDev: BlueDev, ignore: Boolean = false): BlueDev? {
            if (ignore) return blueDev
            val dev = blueDev.dev
            if (noNoName && dev.name == null) return null
            if (find != null && dev.name?.contains(find) != true) return null
            if (!isClassic && minSSID != 0 && minSSID.absoluteValue > blueDev.rssi.absoluteValue) return null
            return blueDev
        }

        fun stopScan() {
            if (isClassic) {
                BluetoothHelper.CLASSIC.stopScan()
            } else {
                BluetoothHelper.stopScan()
            }
        }

        fun startScan(l: OnResultListener<BlueDev>) {
            BluetoothHelper.allConnectedDevs {
                it.forEach { dev -> filter(BlueDev.fromSys(dev))?.let { dev2 -> l.onResult(dev2) } }
                if (isClassic) {
                    BluetoothHelper.CLASSIC.apply { setScannedListener { dev -> filter(BlueDev.from(dev))?.let { dev2 -> l.onResult(dev2) } } }.startScan()
                } else {
                    BluetoothHelper.scanResult { dev -> filter(BlueDev.from(dev))?.let { dev2 -> l.onResult(dev2) } }
                }
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