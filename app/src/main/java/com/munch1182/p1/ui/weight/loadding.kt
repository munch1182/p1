package com.munch1182.p1.ui.weight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.munch1182.p1.ui.theme.PagePadding

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(PagePadding)
            .fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}