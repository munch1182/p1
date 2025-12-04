package com.munch1182.p1.views

import androidx.compose.runtime.Composable
import com.munch1182.lib.AppHelper
import com.munch1182.p1.ui.weight.DefaultFileExplorer

@Composable
fun FileExplorerView() {
    DefaultFileExplorer(AppHelper.filesDir)
}
