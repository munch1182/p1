package com.munch1182.p1.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun Split() = Spacer(Modifier.height(16.dp))

@Composable
fun ButtonDefault(text: String, onClick: () -> Unit) = Button(onClick) { Text(text) }