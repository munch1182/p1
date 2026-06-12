package com.munch1182.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.munch1182.core.ui.theme.Dimens
import com.munch1182.core.ui.theme.P1Theme
import com.munch1182.core.ui.theme.ThemeVM
import com.munch1182.core.ui.theme.paddingPage
import com.munch1182.lib.common.AnalyticsTracker

private object PreviewAnalytics : AnalyticsTracker {
    override fun trackScreen(screenName: String, properties: Map<String, Any>?) {}
    override fun trackEvent(eventName: String, properties: Map<String, Any>?) {}
    override fun trackUserProperty(key: String, value: Any) {}
    override fun identify(userId: String, traits: Map<String, Any>?) {}
    override fun setUserProperties(properties: Map<String, Any>) {}
}

/**
 * 用于组合函数 @Preview 的容器，包裹 P1Theme 与 Surface 背景。
 */
@Composable
fun PreviewContainer(
    modifier: Modifier = Modifier,
    vm: ThemeVM = hiltViewModel(),
    content: @Composable () -> Unit
) {
    P1Theme(vm) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.paddingPage(),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingItem)
            ) {
                content()
            }
        }
    }
}
