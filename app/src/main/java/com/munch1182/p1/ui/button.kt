package com.munch1182.p1.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.munch1182.p1.ui.theme.Dimens


/**
 * 提供一个默认的button
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
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
 * 状态按钮，覆盖“未进行/进行中”以及“空闲/加载中/成功”两种场景。
 * @param text 正常状态文本（未加载/空闲/成功时显示）
 * @param isLoading 是否显示加载进度条（此时禁用点击）
 * @param isSuccess 是否显示成功状态（若为 true 则禁用点击，并保持显示 text）
 * @param modifier 修饰符
 * @param onClick 仅在 !isLoading && !isSuccess 时触发
 */
@Composable
fun StateButton(
    text: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.height(Dimens.HeightBottom),
        enabled = !isLoading && !isSuccess,
        onClick = onClick,
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.CircularSize),
                    strokeWidth = Dimens.StrokeWidth
                )
                Text(
                    text = text,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}


@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "深色模式")
@Preview(showBackground = true, name = "浅色模式")
@Composable
fun PrimaryButtonCombinedPreview() {
    PreviewContainer {
        PrimaryButton(text = "确认跳转", onClick = {})
        PrimaryButton(
            text = "提交订单",
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        )
    }
}
