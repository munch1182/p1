package com.munch1182.feature.bluetooth.connect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.munch1182.core.ui.AccordionLabelItem
import com.munch1182.core.ui.SplitH
import com.munch1182.core.ui.theme.Dimens
import com.munch1182.core.ui.theme.paddingPage
import com.munch1182.lib.bluetooth.le.BLECharacteristic
import com.munch1182.lib.bluetooth.le.BLEServiceInfo
import com.munch1182.lib.bluetooth.le.BluetoothConnectState
import com.munch1182.lib.bluetooth.le.asBleUuid2ShortIfCan
import com.munch1182.lib.bluetooth.le.isConnected
import com.munch1182.lib.bluetooth.le.isDisconnected


data class BLECommonProtocol(
    val name: String,
    val status: String,
    val description: String,
)

/**
 * 蓝牙连接ui及其相关逻辑
 *
 * @param address 蓝牙地址, 注意连接蓝牙设备时不会再去扫描设备是否存在, 而是直接使用此地址构建设备并连接,
 *                因此在调用前需确定设备可连接
 * @param deviceName 蓝牙设备名称
 * @param onConnectToggle 连接状态改变时调用, 参数为连接成功/失败
 */
@Composable
fun BluetoothConnect(
    address: String,
    deviceName: String,
    modifier: Modifier = Modifier,
    vm: BluetoothConnectViewModel = hiltViewModel(),
    onConnectToggle: (Boolean) -> Unit = {},
) {
    var showServices by remember { mutableStateOf(false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val errMsg by vm.errMsg.collectAsStateWithLifecycle("")

    LaunchedEffect(Unit) { vm.toggleConnect(address) }

    LaunchedEffect(state.connectState) {
        if (state.connectState.isConnected) {
            onConnectToggle(true)
        } else if (state.connectState.isDisconnected) {
            onConnectToggle(false)
        }
    }

    Column(
        modifier = Modifier
            .paddingPage()
            .then(modifier)
    ) {
        DeviceHeader(
            name = deviceName,
            address = address,
            mtu = state.mtu,
            connectState = state.connectState,
            onConnectToggle = { vm.toggleConnect(address) },
        )

        SplitH()

        if (errMsg.isEmpty()) {
            if (state.services.isNotEmpty()) {
                ServicesSection(
                    expanded = showServices,
                    onToggle = { showServices = !showServices },
                    services = state.services,
                )
            }
        } else {
            Text(text = errMsg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DeviceHeader(
    name: String,
    address: String,
    mtu: Int,
    connectState: BluetoothConnectState,
    onConnectToggle: () -> Unit,
) {
    val isConnected = connectState.isConnected
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.PaddingPage)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Dimens.PaddingItem))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isConnected) MtuBadge(mtu)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = when (connectState) {
                        BluetoothConnectState.Disconnected -> "连接蓝牙"
                        BluetoothConnectState.Connecting -> "连接中..."
                        BluetoothConnectState.Connected -> "已连接,点击断开"
                        BluetoothConnectState.Disconnecting -> "断开中..."
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.PaddingItem))
                        .clickable(onClick = onConnectToggle)
                        .padding(horizontal = Dimens.PaddingPage, vertical = Dimens.PaddingItemHalf),
                )
            }
        }
    }
}

@Composable
private fun MtuBadge(mtu: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Text(
            "MTU $mtu",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ServicesSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    services: List<BLEServiceInfo>,
) {
    AccordionLabelItem(
        expanded = expanded,
        onToggle = onToggle,
        modifier = Modifier.padding(horizontal = Dimens.PaddingItem),
        title = {
            Text(
                "服务与特征 (${services.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(Dimens.PaddingItem),
            )
        },
        content = {
            LazyColumn {
                items(services, key = { it.uuid }) { service ->
                    ServiceCard(service)
                    Spacer(Modifier.size(Dimens.PaddingItemHalf))
                }
            }
        },
    )
}

@Composable
private fun ServiceCard(service: BLEServiceInfo) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        AccordionLabelItem(
            expanded = expanded,
            onToggle = { expanded = !expanded },
            modifier = Modifier.padding(horizontal = Dimens.PaddingItem),
            space = 0.dp,
            title = {
                Column(modifier = Modifier.padding(horizontal = Dimens.PaddingItem, vertical = Dimens.PaddingPage)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = service.uuid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            content = {
                Column(Modifier.padding(horizontal = Dimens.PaddingItem + Dimens.PaddingItem, vertical = Dimens.PaddingPage)) {
                    service.characteristics.forEach { characteristic ->
                        CharacteristicRow(characteristic)
                    }
                }
            },
        )
    }
}

@Composable
private fun CharacteristicRow(ch: BLECharacteristic) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.PaddingItemHalf),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start // 从左排列，无多余间距
    ) {
        // 左侧 Text：占用剩余空间但“不强制填满”，即宽度 = min(内容宽度, 剩余宽度)
        Text(
            text = ch.uuid.asBleUuid2ShortIfCan(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false) // 关键！
        )

        // 右侧 Column：宽度由内容决定，始终紧挨着 Text 的右侧
        Column(
            modifier = Modifier.padding(start = Dimens.PaddingItem)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ch.properties.forEach { prop -> PropertyChip(prop) }
            }
            val value = ch.value
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = Int.MAX_VALUE,      // 允许换行（如果你希望它不换行，可保持 1）
                    overflow = TextOverflow.Ellipsis // 或 Visible，根据需求
                )
            }
        }
    }
}

@Composable
private fun PropertyChip(property: String) {
    val color = when (property) {
        "READ" -> MaterialTheme.colorScheme.tertiary
        "WRITE", "WRITE_NO_RESPONSE" -> MaterialTheme.colorScheme.primary
        "NOTIFY" -> MaterialTheme.colorScheme.secondary
        "INDICATE" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = property,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}