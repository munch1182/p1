package com.munch1182.p1.views

import androidx.compose.runtime.Composable
import com.munch1182.p1.ui.ClickButton

@Composable
fun TestView() {
    ClickButton("test1") { test1() }
}

private fun test1() {}
