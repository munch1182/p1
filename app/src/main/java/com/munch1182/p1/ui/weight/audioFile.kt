package com.munch1182.p1.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munch1182.p1.ui.IconStateOutlineButton
import com.munch1182.p1.ui.OutlineButtonState
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.theme.PagePadding


// 音频信息数据类
data class AudioFileInfo(
    val fileName: String,
    val fileSize: String,
    val duration: String,
    val format: String,
    val filePath: String,
    val isPlaying: Boolean = false
)

@Composable
fun AudioInfoSheet(
    audioInfo: AudioFileInfo,
    onPlayPauseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PagePadding)
    ) {
        // 标题
        Text(
            text = "音频文件",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SpacerV()

        // 文件信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 文件名
                Text(
                    text = audioInfo.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )

                SpacerV()

                // 详细信息网格
                AudioInfoGrid(audioInfo)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconStateOutlineButton(
                onClick = onPlayPauseClick, modifier = Modifier.weight(1f),
                state = if (audioInfo.isPlaying) {
                    OutlineButtonState(Icons.Default.Pause, "暂停播放", MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                } else {
                    OutlineButtonState(Icons.Default.PlayArrow, "播放音频", Color.Transparent)
                }
            )

            Spacer(modifier = Modifier.width(12.dp))
            IconStateOutlineButton(
                onClick = onDeleteClick, modifier = Modifier.weight(1f),
                state =
                    OutlineButtonState(
                        Icons.Default.Delete, "删除",
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    )

            )
        }
    }
}

@Composable
private fun AudioInfoGrid(audioInfo: AudioFileInfo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 时长
        AudioInfoItem(
            icon = Icons.Default.Schedule,
            label = "时长",
            value = audioInfo.duration
        )

        // 文件大小
        AudioInfoItem(
            icon = Icons.Default.SdStorage,
            label = "大小",
            value = audioInfo.fileSize
        )

        // 格式
        AudioInfoItem(
            icon = Icons.Default.MusicNote,
            label = "格式",
            value = audioInfo.format
        )
    }
}

@Composable
private fun AudioInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}