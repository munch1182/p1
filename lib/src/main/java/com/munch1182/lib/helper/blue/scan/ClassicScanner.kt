package com.munch1182.lib.helper.blue.scan

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresPermission
import com.munch1182.lib.base.newLog
import com.munch1182.lib.helper.blue.BluetoothHelper

class ClassicScanner : BaseScanner() {

    private val receiver = BluetoothFoundReceiver(scanDispatchCallback)

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    override fun startScan() {
        log.logStr("Start Scan")
        scanDispatchCallback.onPreScanStart()
        receiver.register(BluetoothHelper.ctx)
        adapter?.startDiscovery()
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    override fun stopScan() {
        log.logStr("Stop Scan")
        scanDispatchCallback.onPreScanStop()
        adapter?.cancelDiscovery()
    }

    class BluetoothFoundReceiver(private val l: BluetoothScanListener?) : BroadcastReceiver() {

        private val log = BluetoothHelper.log.newLog("FondReceiver")

        fun register(context: Context) {
            log.logStr("Register")
            context.registerReceiver(this, IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothDevice.ACTION_FOUND)
            })
        }

        // 由stopScan的广播自动取消注册
        private fun unregister(context: Context) {
            log.logStr("Unregister")
            context.unregisterReceiver(this)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return

            when (action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    log.logStr("Found: action: $action")
                    l?.onScanStart()
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    log.logStr("Found: action: $action")
                    l?.onScanStop()
                    unregister(BluetoothHelper.ctx)
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val dev = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                    val rssi = intent.getIntExtra(BluetoothDevice.EXTRA_RSSI, 0)
                    log.logStr("Found: ${name}(${dev?.address}), rssi: $rssi)")
                    dev?.let { l?.onScanned(it) }
                }
            }
        }
    }
}