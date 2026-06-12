package com.munch1182.lib.bluetooth.le

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import com.munch1182.lib.android.logger
import com.munch1182.lib.bluetooth.BluetoothEnv
import com.munch1182.lib.bluetooth.IBluetoothEnv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn

/**
 * 在给定的协程作用域中启动蓝牙LE扫描流（实际扫描由 [BLEScanner] 管理）
 */
fun leScanFlow(): Flow<ScanResult> = BLEScanner.Default.scanFlow()

/** BLE 扫描管理器，提供共享扫描流，多收集者共享同一硬件扫描资源 */
@SuppressLint("MissingPermission")
class BLEScanner(private val env: IBluetoothEnv = BluetoothEnv) : IBluetoothEnv by env {

    companion object {
        /** 默认单例实例，用于一般场景 */
        val Default = BLEScanner()
    }

    // 内部固定协程作用域，保证 SharedFlow 生命周期稳定
    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val log = logger()

    @Volatile
    private var currFlow: SharedFlow<ScanResult>? = null

    /**
     * 获取蓝牙 LE 扫描结果流（推荐使用）
     *
     * - 只有在流被收集时才会开始扫描
     * - 多个收集者共享同一扫描结果（不会重复启动硬件扫描）
     * - 当最后一个收集者取消时，自动停止扫描并释放资源
     */
    fun scanFlow(): Flow<ScanResult> {
        val flow = currFlow
        if (flow != null) return flow
        synchronized(this) {
            val existing = currFlow
            if (existing != null) return existing
            val newFlow = createScanFlow().shareIn(
                scope = internalScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), // 无订阅者立即停止
                replay = 0
            )
            currFlow = newFlow
            return newFlow
        }
    }

    private fun stopScan() {
        synchronized(this) { currFlow = null }
    }

    private fun createScanFlow(): Flow<ScanResult> = callbackFlow {
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
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        log.log("start LEScan: ${scanner != null}")
        scanner?.startScan(emptyList(), settings, callback)

        awaitClose {
            scanner?.stopScan(callback)
            log.log("stop LEScan")
            stopScan()   // 清空缓存，下次调用会重建扫描流
        }
    }
}