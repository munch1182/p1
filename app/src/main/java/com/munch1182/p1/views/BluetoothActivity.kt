package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.log
import com.munch1182.lib.base.toast
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.blue.scan
import com.munch1182.lib.helper.blue.scan.BluetoothScanningListener
import com.munch1182.lib.helper.result.isAllGranted
import com.munch1182.lib.helper.result.permission
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithScroll

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
    }

    private fun withScanPermission(canScan: () -> Unit) {
        permission {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }.request {
            if (it.isAllGranted) {
                canScan()
            } else {
                toast("扫描失败")
            }
        }
    }
}

class BluetoothVM : ViewModel() {
    private val log = log()
    private var _scanning = MutableLiveData(false)

    val scanning = _scanning.asLive()
    private val scanningListener = BluetoothScanningListener { _scanning.postValue(it) }
    private val devs = HashMap<String, BluetoothDevice>()

    init {
        BluetoothHelper.addScanningListener(scanningListener)
    }

    @SuppressLint("MissingPermission")
    fun toggleScan() {
        log.logStr("toggleScan")
        if (scanning.value == true) {
            BluetoothHelper.stopScan()
        } else {
            devs.clear()
            BluetoothHelper.scan {
                devs[it.address] = it
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        BluetoothHelper.removeScanningListener(scanningListener)
    }
}