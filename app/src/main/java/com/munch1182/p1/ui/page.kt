package com.munch1182.p1.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.munch1182.p1.ui.theme.Dimens
import com.munch1182.p1.ui.theme.paddingPage

/**
 * 提供一个可滑动的页面，来供没有页面内滑元素的页面来兼容小屏幕设备
 *
 * 不兼容页面内滑动
 *
 * @param applyPadding 是否应用默认页面内边距
 * @param applyVerticalSpace 是否应用默认页面内垂直间距
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