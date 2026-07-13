package com.munch1182.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munch1182.core.ui.theme.Dimens

/**
 * 显示一个左侧标题右侧选项的配置ui
 */
@Composable
fun KeyValueSelectItem(
    key: String,                // 左侧的显示文本
    value: String,              // 右侧的显示文本
    isExpand: Boolean,          // 是否展开
    modifier: Modifier = Modifier, //
    keyView: @Composable (String) -> Unit = { Text(it) }, //
    onClick: () -> Unit = {}
) {
    Surface(shape = RoundedCornerShape(50), shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(Dimens.PaddingItem)
                .then(modifier), //
            verticalAlignment = Alignment.CenterVertically, //
            horizontalArrangement = Arrangement.SpaceBetween //
        ) {
            keyView(key)
            ValueSpinner(value, isExpand)
        }
    }
}


@Composable
fun ValueSpinner(
    value: String,                // 要显示的值，如 "44100"
    isExpand: Boolean,          // 是否展开
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,                          //
        shape = RoundedCornerShape(50),      // 圆角矩形 (对应 border-radius)
        color = Color(0xFFF0F0F0),                   // 背景色 (对应 #f0f0f0)
        shadowElevation = 0.dp                       // 无阴影，保持扁平
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, //
            horizontalArrangement = Arrangement.Center //
        ) {
            // 数值文本
            Text(text = value, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A)) // 深色文字
            // 下拉箭头图标
            Icon(
                imageVector = if (!isExpand) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = null,            //
                modifier = Modifier.size(18.dp),     //
                tint = Color(0xFF666666)             // 灰色图标
            )
        }
    }
}