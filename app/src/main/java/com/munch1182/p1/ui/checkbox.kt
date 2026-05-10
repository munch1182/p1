package com.munch1182.p1.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Checkbox(name: String, checked: Boolean, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit = { }) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Checkbox(checked, onCheckedChange)
        Text(text = name)
    }
}