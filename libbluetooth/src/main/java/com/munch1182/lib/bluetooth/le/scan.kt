package com.munch1182.lib.bluetooth.le

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import com.munch1182.lib.base.log
import com.munch1182.lib.bluetooth.BluetoothIBluetoothEnv
import com.munch1182.lib.bluetooth.IBluetoothEnv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 蓝牙扫描并收集结果
 *
 * 使用取消协程的方法取消扫描
 */
suspend fun CoroutineScope.leScanFlow(): Flow<ScanResult> = BLEScanner.scanFlow(this)

@SuppressLint("MissingPermission")
object BLEScanner : IBluetoothEnv by BluetoothIBluetoothEnv {

    private val log = log()
    private val lock = Mutex()
    private var currFlow: SharedFlow<ScanResult>? = null

    /**
     * 扫描蓝牙设备
     *
     * 只有在[Flow.collect]后才会开始扫描;
     * 可以在多个收集者共享扫描数据，并且在没有收集者时停止扫描并销毁
     */
    suspend fun scanFlow(scope: CoroutineScope): Flow<ScanResult> {
        val flow = lock.withLock { currFlow }
        if (flow != null) return flow
        val newFlow = newSharedScanFlow.shareIn(
            scope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), // 立即停止
            replay = 0 // 不重放旧数据
        )
        lock.withLock { currFlow = newFlow }
        return newFlow
    }

    private fun stopScan() {
        currFlow = null
    }

    // 创建共享的扫描流
    private val newSharedScanFlow = callbackFlow {
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { trySend(it) }
            }

            override fun onBatchScanResults(results: List<ScanResult?>?) {
                results?.forEach { result -> result?.let { trySend(it) } }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("onScanFailed: $errorCode"))
            }
        }

        val scanner = adapter?.bluetoothLeScanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        log.log("start LEScan")
        scanner?.startScan(emptyList(), settings, callback)

        awaitClose {
            scanner?.stopScan(callback)
            log.log("stop LEScan")
            stopScan()
        }
    }
}
