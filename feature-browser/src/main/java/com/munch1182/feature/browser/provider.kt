package com.munch1182.feature.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.munch1182.core.router.DeepLinkRoutes
import com.munch1182.core.ui.PrimaryButton
import com.munch1182.core.ui.theme.paddingPage
import com.munch1182.lib.android.navigate2DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.parameters.CodeGenVisibility

@NavGraph<ExternalModuleGraph>
annotation class FeatureBrowserGraph

@Destination<FeatureBrowserGraph>(start = true, visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun BrowserScreen() {
    Column(Modifier.paddingPage()) {
        PrimaryButton("启动") { navigate2DeepLink(DeepLinkRoutes.BROWSER.HOME_URI) }
    }
}
