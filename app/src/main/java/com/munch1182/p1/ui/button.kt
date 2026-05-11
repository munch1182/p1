package com.munch1182.p1.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
