package com.munch1182.p1.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 默认边距
val PagePadding = 16.dp
val PagePaddingHalf = PagePadding / 2

val PagePaddingModifier = Modifier.padding(PagePadding)
val ItemPadding = Modifier.padding(horizontal = PagePadding, vertical = PagePaddingHalf)

val FontSize = TextUnit.Unspecified
val FontTitleSize = 16.sp
val FontManySize = 12.sp
val FontDescSize = 12.sp
