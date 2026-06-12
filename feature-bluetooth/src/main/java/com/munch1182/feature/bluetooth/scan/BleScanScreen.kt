package com.munch1182.feature.bluetooth.scan

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.munch1182.core.ui.RunningStateButton
import com.munch1182.core.ui.SplitH
import com.munch1182.core.ui.dialog.DialogFactory
import com.munch1182.core.ui.permission.checkBluetoothPermission
import com.munch1182.core.ui.theme.Dimens
import com.munch1182.core.ui.theme.paddingPage
import com.munch1182.lib.bluetooth.BLEScanRecordHelper.BlueRecord

@Composable
fun BluetoothScan(
    modifier: Modifier = Modifier,
    vm: BleScanViewModel = hiltViewModel(),
    onDeviceClick: (BluetoothDevice) -> Unit = {},
) {
    val devices by vm.devices.collectAsStateWithLifecycle()
    val isScanning by vm.isScanning.collectAsStateWithLifecycle()

    Column(
        Modifier
            .paddingPage()
            .then(modifier)
    ) {
        RunningStateButton(
            isScanning, //
            text = if (isScanning) "停止扫描" else "开始扫描", //
            modifier = Modifier.fillMaxWidth(), //
            onClick = {
                checkBluetoothPermission { if (it) vm.toggleScan() }
            })

        if (isScanning && devices.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text("正在扫描附近的 BLE 设备...", modifier = Modifier.paddingPage())
            }
        }

        SplitH()

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.PaddingItem)) {
            items(devices, key = { it.device.address }) { device ->
                DeviceCard(
                    scanned = device,
                    onClick = { onDeviceClick(device.device) },
                    onLongClick = { showScanRecord(device) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCard(scanned: ScannedDevice, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(Dimens.PaddingPage),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Dimens.PaddingItem)
            ) {
                Text(
                    text = scanned.name ?: "未知设备",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = scanned.device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${scanned.rssi} dBm",
                style = MaterialTheme.typography.bodySmall,
                color = if (scanned.rssi > -70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun showScanRecord(scanned: ScannedDevice) {
    DialogFactory.newBottom {
        Column(modifier = Modifier.paddingPage()) {
            Text(
                "广播数据 · ${scanned.name ?: scanned.device.address}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Dimens.PaddingItem))

            scanned.rawBytes?.let { bytes ->
                Text("原始数据", style = MaterialTheme.typography.labelLarge)
                Text(
                    bytes.toHexString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.PaddingItem))
            }

            Text("解析字段 (${scanned.records.size})", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))

            if (scanned.records.isEmpty()) {
                Text("无广播数据", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(scanned.records) { record -> RecordRow(record) }
                }
            }
        }
    }.show()
}

@Composable
private fun RecordRow(record: BlueRecord) {
    Column(modifier = Modifier.padding(vertical = Dimens.PaddingItem)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                record.typeStr(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "0x${record.type.toHexString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            record.value2StrIfTypeCan(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            record.value.toHexString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
    }
}
