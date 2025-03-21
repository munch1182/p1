package com.munch1182.p1.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun Split() = Spacer(Modifier.height(16.dp))

@Composable
fun ButtonDefault(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) = Button(onClick, modifier) { Text(text) }

@Composable
fun CheckBoxLabel(
    label: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .padding(end = 16.dp)
        .clickable { onCheckedChange?.invoke(!checked) },
    verticalAlignment = Alignment.CenterVertically
) {
    Checkbox(checked, onCheckedChange)
    Text(label)
}