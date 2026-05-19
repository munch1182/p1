package com.munch1182.p1.ui

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
import com.munch1182.p1.ui.theme.Dimens


/**
 * 提供一个默认的button
 */
@Composable
fun PrimaryButton(
    text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit
) {
    Button(
        modifier = modifier.height(Dimens.HeightBottom), // 统一项目中的按钮高度
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * 定义按钮在某个状态下的完整视觉配置
 * @param text 按钮上显示的文本
 * @param containerColor 按钮背景色
 * @param contentColor 按钮内容（文字/进度条）颜色
 * @param showProgress 是否显示加载进度条（优先级高于文本）
 * @param enabled 按钮是否可点击
 */
data class ButtonStateConfig(
    val text: String, val containerColor: Color, val contentColor: Color, val showProgress: Boolean = false, val enabled: Boolean = true
)

/**
 * 支持任意多状态的按钮组件
 * @param state 当前状态（可以是任何类型，如枚举、密封类等）
 * @param onClick 点击回调（由外层传入，内部不自动禁用，外层可依据状态决定是否响应）
 * @param config 根据当前状态提供 [ButtonStateConfig] 的 lambda
 * @param modifier 修饰符
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
        modifier = modifier, onClick = onClick, enabled = cfg.enabled, colors = ButtonDefaults.buttonColors(
            containerColor = cfg.containerColor, contentColor = cfg.contentColor
        )
    ) {
        if (cfg.showProgress) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.CircularSize), strokeWidth = Dimens.StrokeWidth, color = cfg.contentColor
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

enum class RunButtonState {
    IDLE,       // 默认状态
    STARTING,   // 启动中（显示进度条）
    RUNNING     // 运行中
}

/**
 * 便捷函数：直接使用默认三种状态的按钮
 * @param state [RunButtonState] 当前状态
 * @param onClick 点击回调
 * @param modifier 修饰符
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
                    text = text ?: "默认", //
                    containerColor = containerColor ?: colorScheme.primary, //
                    contentColor = colorScheme.onPrimary, //
                    showProgress = false, //
                )

                RunButtonState.STARTING -> ButtonStateConfig(
                    text = text ?: "启动中", //
                    containerColor = containerColor ?: colorScheme.surfaceVariant, //
                    contentColor = colorScheme.onSurfaceVariant, //
                    showProgress = true, //
                )

                RunButtonState.RUNNING -> ButtonStateConfig(
                    text = text ?: "运行中", //
                    containerColor = containerColor ?: colorScheme.errorContainer, //
                    contentColor = colorScheme.onSecondaryContainer, //
                    showProgress = false, //
                )
            }
        }, modifier = modifier
    )
}


/**
 * 便捷函数：直接使用默认两种状态的按钮
 * @param isRunning 当前是否运行
 * @param onClick 点击回调
 * @param modifier 修饰符
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
