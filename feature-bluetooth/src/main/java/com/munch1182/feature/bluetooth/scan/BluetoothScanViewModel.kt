package com.munch1182.feature.bluetooth.scan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.android.logger
import com.munch1182.lib.bluetooth.BLEScanRecordHelper
import com.munch1182.lib.bluetooth.BLEScanRecordHelper.BlueRecord
import com.munch1182.lib.bluetooth.le.leScanFlow
import com.munch1182.lib.common.AnalyticsTracker
import com.munch1182.lib.common.launchIO
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlin.time.Duration.Companion.milliseconds

data class ScannedDevice(
    val device: BluetoothDevice,
    val name: String?,
    val records: List<BlueRecord>,
    val rawBytes: ByteArray?,
    val rssi: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScannedDevice

        if (rssi != other.rssi) return false
        if (device != other.device) return false
        if (name != other.name) return false
        if (records != other.records) return false
        if (!rawBytes.contentEquals(other.rawBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rssi
        result = 31 * result + device.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + records.hashCode()
        result = 31 * result + (rawBytes?.contentHashCode() ?: 0)
        return result
    }

}

@HiltViewModel
class BluetoothScanViewModel @Inject constructor(private val analytics: AnalyticsTracker) : ViewModel() {

    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null
    private val deviceMap = mutableMapOf<String, ScannedDevice>()

    private val log = logger()

    companion object {
        private const val EVENT_START_SCAN = "BT_START_SCAN"
        private const val EVENT_STOP_SCAN = "BT_STOP_SCAN"
    }

    fun toggleScan() {
        if (isScanning.value) {
            stopScan()
        } else {
            startScan()
        }
    }

    @OptIn(FlowPreview::class)
    private fun startScan() {
        analytics.trackEvent(EVENT_START_SCAN)
        _isScanning.value = true
        deviceMap.clear()
        _devices.value = emptyList()

        val scanBuffMap = mutableMapOf<String, ScanResult>()

        scanJob = viewModelScope.launchIO {
            leScanFlow()
                .onEach { scanBuffMap[it.device.address] = it }
                .sample(600.milliseconds)
                .map { scanBuffMap.values.toList() }
                .catch { _isScanning.value = false }
                .collect(::addOrUpdateDevice)
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
        analytics.trackEvent(EVENT_STOP_SCAN)
    }

    @SuppressLint("MissingPermission")
    private fun addOrUpdateDevice(scanned: List<ScanResult>) {
        scanned.forEach { result ->
            val dev = result.device
            val rawBytes = result.scanRecord?.bytes
            val records = rawBytes?.let { BLEScanRecordHelper.parseScanRecord(it) } ?: emptyList()
            val name = result.scanRecord?.deviceName ?: runCatching { dev.name }.getOrNull()

            deviceMap[dev.address] = ScannedDevice(
                device = dev,
                name = name,
                records = records,
                rawBytes = rawBytes,
                rssi = result.rssi,
            )
        }
        _devices.value = deviceMap.values.toList()
    }

    override fun onCleared() {
        stopScan()
    }
}
