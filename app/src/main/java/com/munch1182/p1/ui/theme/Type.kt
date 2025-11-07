package com.munch1182.p1.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)


// 标准页面内边距
val PagePadding = 16.dp

val PagePaddingHalf = PagePadding / 2

val PagePaddingHalfLarge = 12.dp

// 标准的页面内边距修饰符
val PagePaddingModifier = Modifier.padding(PagePadding)

// 水平方向的内边距
val HorizontalPagePadding = PagePadding
val HorizontalPagePaddingModifier = Modifier.padding(horizontal = PagePadding)

// 垂直方向的内边距
val VerticalPagePadding = PagePadding
val VerticalPagePaddingModifier = Modifier.padding(vertical = PagePadding)

val TextSm = 12.sp
val TextSize = 14.sp
val TextLg = 18.sp

val CornerDef = 8.dp


val FontManySize = TextSm
