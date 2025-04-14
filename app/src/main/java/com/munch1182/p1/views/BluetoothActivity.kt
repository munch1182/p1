package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.base.toast
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.blue.scan
import com.munch1182.lib.helper.blue.scan.BluetoothScanningListener
import com.munch1182.lib.helper.isLocationProvider
import com.munch1182.lib.helper.result.ifAllGranted
import com.munch1182.lib.helper.result.ifTrue
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.result.permissions
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper.intentBlueScan
import com.munch1182.p1.base.DialogHelper.permissionBlueScan
import com.munch1182.p1.base.DialogHelper.permissionDialog
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.FontTitleSize
import kotlinx.coroutines.isActive
import java.util.concurrent.LinkedBlockingQueue

class BluetoothActivity : BaseActivity() {
    private val vm by viewModels<BluetoothVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll { Views() }
    }

    @Composable
    private fun Views() {
        val isScanning by vm.scanning.observeAsState(false)

        ClickButton(if (!isScanning) "扫描" else "停止扫描") { withScanPermission { vm.toggleScan() } }

        Devs()
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun Devs() {
        val devs by vm.devices.observeAsState(arrayOf())
        LazyColumn {
            items(devs.size) {
                val blue = devs[it]
                Column {
                    Text(blue.name ?: "", color = Color.Blue, fontSize = FontTitleSize)
                    Text("(${blue.address})")
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
        permissions(p)
            .dialogWhen(permissionDialog("蓝牙", "扫描蓝牙"))
            .manualIntent()
            .ifAllGranted()
            .judge { BluetoothHelper.isBlueOn }
            .intent(BluetoothHelper.enableBlueIntent())
            .ifTrue()
            .judge { isLocationProvider }
            .intent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            .dialogWhen(intentBlueScan())
            .ifTrue()
            .permission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) // 定位打开此权限才会判断为true
            .dialogWhen(permissionBlueScan())
            .ifAllGranted()
            .requestAll { if (it) canScan() else toast("扫描失败！") }
    }
}

class BluetoothVM : ViewModel() {
    private val log = log()
    private var _scanning = MutableLiveData(false)
    private var _devs = MutableLiveData<Array<BluetoothDevice>>(arrayOf())

    val scanning = _scanning.asLive()
    val devices = _devs.asLive()

    private val scanningListener = BluetoothScanningListener { _scanning.postValue(it) }
    private val devs = HashMap<String, BluetoothDevice>()
    private val runner = LinkedBlockingQueue<Runnable>()

    init {
        BluetoothHelper.addScanningListener(scanningListener)
        viewModelScope.launchIO {
            while (coroutineContext.isActive) {
                runner.take().run()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun toggleScan() {
        log.logStr("toggleScan")
        if (scanning.value == true) {
            BluetoothHelper.stopScan()
        } else {
            devs.clear()
            BluetoothHelper.scan {
                runner.put {
                    devs[it.address] = it
                    _devs.postValue(devs.values.toTypedArray())
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        BluetoothHelper.removeScanningListener(scanningListener)
    }
}