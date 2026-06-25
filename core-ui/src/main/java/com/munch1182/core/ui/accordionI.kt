package com.munch1182.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import com.munch1182.core.ui.theme.Dimens

/**
 * 可折叠的内容区域组件。
 */
@Composable
fun AccordionItem(
    expanded: Boolean, modifier: Modifier = Modifier, space: Dp = Dimens.PaddingItem, onToggle: () -> Unit, title: @Composable () -> Unit, content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(space))
                .clickable(onClick = onToggle)
        ) {
            title()
        }
        Spacer(Modifier.height(space))
        AnimatedVisibility(visible = expanded) {
            Spacer(Modifier.height(space))
            content()
        }
    }
}

/**
 * 可折叠的内容区域组件，标题右侧带旋转箭头图标。
 *
 * @param modifier 标题内容的样式, 即title+icon的容器
 * @param space 标题和内容之间的间距
 */
@Composable
fun AccordionLabelItem(
    expanded: Boolean, //
    modifier: Modifier = Modifier, //
    isEmptyContent: Boolean = false, //
    space: Dp = Dimens.PaddingItem, //
    onToggle: () -> Unit = {}, //
    title: @Composable () -> Unit, //
    content: @Composable () -> Unit //
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f, label = "arrow_rotation"
    )
    AccordionItem(expanded, Modifier, space = space, onToggle = onToggle, title = {
        Row(
            modifier = modifier, verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) { title() }
            if (!isEmptyContent) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier
                        .size(Dimens.Medium)
                        .rotate(rotation)
                )
            }
        }
    }, content = content)
}
