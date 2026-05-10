package com.munch1182.p1.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.munch1182.p1.ui.theme.Dimens

/**
 * 创建默认的垂直间隔
 */
@Composable
fun SplitH() = Spacer(Modifier.height(Dimens.PaddingItem))

/**
 * 创建默认的水平间隔
 */
@Composable
fun SplitW() = Spacer(Modifier.width(Dimens.PaddingItem)) // 使用Dimens中定义的PaddingItem作为宽度值