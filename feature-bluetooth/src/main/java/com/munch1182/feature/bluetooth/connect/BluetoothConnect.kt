package com.munch1182.feature.bluetooth.connect

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.munch1182.core.ui.AccordionLabelItem
import com.munch1182.core.ui.SplitH
import com.munch1182.core.ui.theme.Dimens
import com.munch1182.core.ui.theme.paddingPage

//region Mock Data Models

data class MockCharacteristic(
    val uuid: String,
    val properties: List<String>,
    val value: String? = null,
)

data class MockService(
    val uuid: String,
    val name: String,
    val characteristics: List<MockCharacteristic>,
)

data class MockTestProtocol(
    val name: String,
    val status: String,
    val description: String,
)

//endregion

//region Mock Data

private val mockServices = listOf(
    MockService(
        uuid = "00001800-0000-1000-8000-00805F9B34FB",
        name = "Generic Access",
        characteristics = listOf(
            MockCharacteristic("2A00", listOf("Read", "Write"), "LILYGO T-Display"),
            MockCharacteristic("2A01", listOf("Read", "Notify"), "0x0600"),
            MockCharacteristic("2A04", listOf("Read", "Write", "Indicate"), "0x0014-0x0054"),
        ),
    ),
    MockService(
        uuid = "0000180A-0000-1000-8000-00805F9B34FB",
        name = "Device Information",
        characteristics = listOf(
            MockCharacteristic("2A29", listOf("Read"), "LILYGO"),
            MockCharacteristic("2A24", listOf("Read"), "T-Display"),
            MockCharacteristic("2A26", listOf("Read"), "1.0.0"),
        ),
    ),
    MockService(
        uuid = "0000180F-0000-1000-8000-00805F9B34FB",
        name = "Battery Service",
        characteristics = listOf(
            MockCharacteristic("2A19", listOf("Read", "Notify"), "85%"),
        ),
    ),
    MockService(
        uuid = "0000FFE0-0000-1000-8000-00805F9B34FB",
        name = "Custom Service",
        characteristics = listOf(
            MockCharacteristic("FFE1", listOf("Write", "Indicate"), null),
            MockCharacteristic("FFE2", listOf("Read", "Write", "Notify"), "0x0A-0x0B-0x0C"),
        ),
    ),
)

private val mockTestProtocols = listOf(
    MockTestProtocol(
        name = "Ping Protocol",
        status = "等待连接",
        description = "发送 Ping 到设备并等待响应，超时 5s",
    ),
    MockTestProtocol(
        name = "Echo Protocol",
        status = "未启动",
        description = "回显所有接收到的数据包，用于测试数据通道",
    ),
)

//endregion

@Composable
fun BluetoothConnect(
    address: String,
    modifier: Modifier = Modifier,
    deviceName: String = "Unknown",
    mtu: Int = 512,
    isConnected: Boolean = false,
    services: List<MockService> = mockServices,
    testProtocols: List<MockTestProtocol> = mockTestProtocols,
    onConnectToggle: () -> Unit = {},
) {
    var showServices by remember { mutableStateOf(true) }
    var showTestProtocols by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .paddingPage()
        .then(modifier)) {
        DeviceHeader(
            name = deviceName,
            address = address,
            mtu = mtu,
            isConnected = isConnected,
            onConnectToggle = onConnectToggle,
        )

        SplitH()

        ServicesSection(
            expanded = showServices,
            onToggle = { showServices = !showServices },
            services = services,
        )

        SplitH()

        TestProtocolSection(
            expanded = showTestProtocols,
            onToggle = { showTestProtocols = !showTestProtocols },
            protocols = testProtocols,
        )
    }
}

@Composable
private fun DeviceHeader(
    name: String,
    address: String,
    mtu: Int,
    isConnected: Boolean,
    onConnectToggle: () -> Unit,
) {
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
                MtuBadge(mtu)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.PaddingItem),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = if (isConnected) "断开" else "连接",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Dimens.PaddingItem, vertical = Dimens.PaddingItemHalf),
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
    services: List<MockService>,
) {
    AccordionLabelItem(
        expanded = expanded,
        onToggle = onToggle,
        title = {
            Text(
                "服务与特征 (${services.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.paddingPage(),
            )
        },
        content = {
            LazyColumn {
                items(services, key = { it.uuid }) { service ->
                    ServiceCard(service)
                }
            }
        },
    )
}

@Composable
private fun ServiceCard(service: MockService) {
    var expanded by remember { mutableStateOf(false) }

    AccordionLabelItem(
        expanded = expanded,
        modifier = Modifier.padding(start = Dimens.PaddingItem, end = Dimens.PaddingItem),
        onToggle = { expanded = !expanded },
        title = {
            Column(modifier = Modifier.padding(vertical = Dimens.PaddingItem)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
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
            Column(modifier = Modifier.padding(start = Dimens.PaddingPage)) {
                service.characteristics.forEach { characteristic ->
                    CharacteristicRow(characteristic)
                }
            }
        },
    )
}

@Composable
private fun CharacteristicRow(ch: MockCharacteristic) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.PaddingItem / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ch.uuid,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(48.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.PaddingItem),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ch.properties.forEach { prop -> PropertyChip(prop) }
            }
            if (ch.value != null) {
                Text(
                    text = ch.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PropertyChip(property: String) {
    val color = when (property) {
        "Read" -> MaterialTheme.colorScheme.tertiary
        "Write" -> MaterialTheme.colorScheme.primary
        "Notify" -> MaterialTheme.colorScheme.secondary
        "Indicate" -> MaterialTheme.colorScheme.error
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

@Composable
private fun TestProtocolSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    protocols: List<MockTestProtocol>,
) {
    AccordionLabelItem(
        expanded = expanded,
        onToggle = onToggle,
        title = {
            Row(
                modifier = Modifier.paddingPage(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.width(Dimens.PaddingItem))
                Text(
                    "测试协议 (${protocols.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        content = {
            Column {
                protocols.forEach { protocol -> TestProtocolRow(protocol) }
                Spacer(Modifier.padding(vertical = Dimens.PaddingItem / 2))

                AnimatedVisibility(visible = protocols.isEmpty()) {
                    Text(
                        text = "暂无已注册的测试协议",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.paddingPage(),
                    )
                }
            }
        },
    )
}

@Composable
private fun TestProtocolRow(protocol: MockTestProtocol) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingPage, vertical = Dimens.PaddingItem / 2),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.PaddingPage),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(protocol.name, style = MaterialTheme.typography.labelLarge)
                Text(
                    protocol.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
            ) {
                Text(
                    protocol.status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
