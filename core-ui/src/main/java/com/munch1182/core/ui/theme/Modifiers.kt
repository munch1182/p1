package com.munch1182.core.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** 应用默认页面内边距。 */
fun Modifier.paddingPage() = padding(Dimens.PaddingPage)

fun Modifier.paddingItem(horizontal: Dp = Dimens.PaddingPage, vertical: Dp = Dimens.PaddingItem) = padding(horizontal, vertical)
