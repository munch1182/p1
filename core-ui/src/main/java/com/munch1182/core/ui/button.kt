package com.munch1182.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.munch1182.core.ui.theme.Dimens

/**
 * 统一高度的主按钮，使用项目默认样式。
 */
@Composable
fun PrimaryButton(
    text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit
) {
    Button(
        modifier = modifier.height(Dimens.HeightBottom),
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * 按钮在某个状态下的视觉配置。
 *
 * @param showProgress 为 true 时显示加载进度条（优先级高于文字）
 */
data class ButtonStateConfig(
    val text: String, //
    val containerColor: Color , //
    val contentColor: Color, //
    val showProgress: Boolean = false, //
    val enabled: Boolean = true //
)

/**
 * 支持任意自定义状态的按钮，通过 [config] 将状态映射到 [ButtonStateConfig]。
 */
@Composable
fun <S> StateButton(
    state: S,
    config: (S) -> ButtonStateConfig,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cfg = config(state)
    Button(
        modifier = modifier.height(Dimens.HeightBottom),
        onClick = onClick,
        enabled = cfg.enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = cfg.containerColor, contentColor = cfg.contentColor
        )
    ) {
        if (cfg.showProgress) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.CircularSize),
                    strokeWidth = Dimens.StrokeWidth,
                    color = cfg.contentColor
                )
                Text(
                    text = cfg.text, modifier = Modifier.padding(start = Dimens.PaddingItem), style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            Text(text = cfg.text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * 三种预设按钮状态。
 */
enum class RunButtonState {
    IDLE, STARTING, RUNNING
}

/**
 * 基于 [RunButtonState] 的三态按钮（空闲/启动中/运行中）。
 */
@Composable
fun RunStateButton(
    state: RunButtonState,
    modifier: Modifier = Modifier,
    text: String? = null,
    containerColor: Color? = null,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    StateButton(
        state = state, onClick = onClick, config = {
            when (state) {
                RunButtonState.IDLE -> ButtonStateConfig(
                    text = text ?: "默认",
                    containerColor = containerColor ?: colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    showProgress = false,
                )

                RunButtonState.STARTING -> ButtonStateConfig(
                    text = text ?: "启动中",
                    containerColor = containerColor ?: colorScheme.surfaceVariant,
                    contentColor = colorScheme.onSurfaceVariant,
                    showProgress = true,
                )

                RunButtonState.RUNNING -> ButtonStateConfig(
                    text = text ?: "运行中",
                    containerColor = containerColor ?: colorScheme.errorContainer,
                    contentColor = colorScheme.onSecondaryContainer,
                    showProgress = false,
                )
            }
        }, modifier = modifier
    )
}

/**
 * 基于布尔值的双态按钮（运行中/空闲）。
 */
@Composable
fun RunningStateButton(
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    text: String? = null,
    containerColor: Color? = null,
    onClick: () -> Unit,
) {
    val state = if (isRunning) RunButtonState.RUNNING else RunButtonState.IDLE
    RunStateButton(
        state = state, onClick = onClick, modifier = modifier, text = text, containerColor = containerColor
    )
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "深色模式")
@Preview(showBackground = true, name = "浅色模式")
@Composable
fun PrimaryButtonCombinedPreview() {
    PreviewContainer {
        PrimaryButton(text = "确认跳转", onClick = {})
        PrimaryButton(
            text = "提交订单", modifier = Modifier.fillMaxWidth(), onClick = {})
    }
}
