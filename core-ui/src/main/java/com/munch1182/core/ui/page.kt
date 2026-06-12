package com.munch1182.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.munch1182.core.ui.theme.Dimens
import com.munch1182.core.ui.theme.paddingPage

/**
 * 可垂直滚动的页面容器，兼容小屏设备。
 *
 * 不兼容页面内嵌套滚动场景（如 LazyColumn）。
 */
@Composable
fun ScrollPage(
    modifier: Modifier = Modifier,
    applyPadding: Boolean = true,
    applyVerticalSpace: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .then(if (applyPadding) Modifier.paddingPage() else Modifier),
        verticalArrangement = if (applyVerticalSpace) Arrangement.spacedBy(Dimens.PaddingItem) else Arrangement.Top
    ) { content() }
}
