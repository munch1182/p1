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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
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
import com.munch1182.lib.base.toast
import com.munch1182.lib.helper.blue.BluetoothHelper
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
import com.munch1182.p1.ui.CheckBoxWithLabel
import com.munch1182.p1.ui.ClickButton
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
    private val vm by viewModels<BluetoothVM>()

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
        val isScanning by vm.scanning.observeAsState(false)
        val scanFilter by vm.scanFilter.observeAsState(BluetoothVM.ScanFilter())

        Column(PagePaddingModifier) {
            CheckBoxWithLabel("经典蓝牙", scanFilter.isClassic) { vm.updateScanFilter(scanFilter.newClassic()) }
            CheckBoxWithLabel("忽略没有名称的蓝牙", scanFilter.ignoreName) { vm.updateScanFilter(scanFilter.newIgnoreName()) }
            ClickButton(if (!isScanning) "扫描" else "停止扫描") { withScanPermission { vm.toggleScan() } }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun Devs() {
        val devs by vm.devices.observeAsState(arrayOf())
        LazyColumn {
            items(devs.size) {
                val blue = devs[it]
                Column {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.Bottom
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
                    HorizontalDivider()
                }
            }
        }
    }

    private fun withScanPermission(canScan: () -> Unit) {
        val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH)
        }
        permissions(p).dialogWhen(permissionDialog("蓝牙", "扫描蓝牙")).manualIntent().ifAllGranted().judge { BluetoothHelper.isBlueOn }.intent(BluetoothHelper.enableBlueIntent()).ifTrue().judge { isLocationProvider }.intent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            .dialogWhen(intentBlueScan()).ifTrue().permission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) // 定位打开此权限才会判断为true
            .dialogWhen(permissionBlueScan()).ifAllGranted().requestAll { if (it) canScan() else toast("扫描失败！") }
    }
}

class BluetoothVM : ViewModel() {
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
        val filter = _filter.value!!
        if (scanning.value == true) {
            filter.stopScan()
        } else {
            devs.clear()
            filter.startScan {
                runner.put {
                    devs[it.address] = it
                    _devs.postValue(devs.values.toTypedArray())
                }
            }
        }
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
    data class BlueDev(val dev: BluetoothDevice, val rssi: Int, val isClassic: Boolean = false) {
        val name: String? get() = dev.name
        val address: String get() = dev.address
        val rssiStr: String? get() = if (isClassic) null else "$rssi dBm"

        companion object {
            fun from(dev: BluetoothDevice, rssi: Int = 0) = BlueDev(dev, rssi, true)
            fun from(scan: ScanResult) = BlueDev(scan.device, scan.rssi)
        }
    }

    @SuppressLint("MissingPermission")
    data class ScanFilter(val ignoreName: Boolean = true, val find: String? = null, val minSSID: Int = 0, val isClassic: Boolean = false) {

        fun newIgnoreName() = ScanFilter(ignoreName = !ignoreName, find = find, minSSID = minSSID)
        fun newClassic() = ScanFilter(ignoreName = ignoreName, find = find, minSSID = minSSID, isClassic = !isClassic)

        fun filter(blueDev: BlueDev): BlueDev? {
            val dev = blueDev.dev
            if (ignoreName && dev.name == null) return null
            if (find != null && dev.name?.contains(find) != true) return null
            if (!isClassic && minSSID != 0 && minSSID.absoluteValue > blueDev.rssi) return null
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
            BluetoothHelper.bondDevs.forEach { l.onResult(BlueDev.from(it)) }
            if (isClassic) {
                BluetoothHelper.CLASSIC.apply { setScannedListener { filter(BlueDev.from(it))?.let { dev -> l.onResult(dev) } } }.startScan()
            } else {
                BluetoothHelper.scanResult { filter(BlueDev.from(it))?.let { dev -> l.onResult(dev) } }
            }
        }
    }
}