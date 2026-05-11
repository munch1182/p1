package com.munch1182.p1.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import com.munch1182.p1.ui.theme.Dimens

/**
 * 提供一个可折叠部分的类手风琴组件
 */
@Composable
fun AccordionItem(
    expanded: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit, title: @Composable () -> Unit, content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.clickable(onClick = onToggle)) {
            title()
        }
        AnimatedVisibility(visible = expanded) { content() }
    }
}

/**
 * 提供一个可折叠部分的类手风琴组件, 其title自带图标
 */
@Composable
fun AccordionLabelItem(
    expanded: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit, title: @Composable () -> Unit, content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f, label = "arrow_rotation"
    )
    AccordionItem(expanded, Modifier, onToggle, title = {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            title()
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier
                    .size(Dimens.Medium)
                    .rotate(rotation)
            )
        }
    }, content)
}