package com.munch1182.p1.ui.weight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.munch1182.p1.ui.theme.PagePaddingModifier

@Composable
fun Loading() {
    Box(
        modifier = PagePaddingModifier.fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}