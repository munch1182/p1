package com.munch1182.p1.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.munch1182.p1.ui.theme.paddingPage

/**
 * 提供一个可滑动的页面，来供没有页面内滑元素的页面来兼容小屏幕设备
 *
 * 不兼容页面内滑动
 */
@Composable
fun ScrollPage(modifier: Modifier = Modifier, applyPadding: Boolean = true, content: @Composable () -> Unit) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .then(if (applyPadding) Modifier.paddingPage() else Modifier)
    ) { content() }
}