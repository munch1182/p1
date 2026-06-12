package com.munch1182.core.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.munch1182.core.ui.theme.Dimens

/** 默认垂直间距（[Dimens.PaddingItem] 高度）。 */
@Composable
fun SplitH() = Spacer(Modifier.height(Dimens.PaddingItem))

/** 默认水平间距（[Dimens.PaddingItem] 宽度）。 */
@Composable
fun SplitW() = Spacer(Modifier.width(Dimens.PaddingItem))
