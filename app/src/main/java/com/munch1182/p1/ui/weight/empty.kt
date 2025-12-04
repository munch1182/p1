package com.munch1182.p1.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.munch1182.p1.ui.theme.PagePaddingHalf
import com.munch1182.p1.ui.theme.PagePaddingModifier

@Composable
fun EmptyContent(msg: String = "当前为空") {
    Box(
        modifier = PagePaddingModifier.fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PagePaddingHalf)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.LightGray
            )
            Text(msg, color = Color.Gray)
        }
    }
}