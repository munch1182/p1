package com.munch1182.p1.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.munch1182.lib.helper.currAct
import com.munch1182.p1.ui.theme.P1Theme
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingModifier
import kotlin.reflect.KClass

// ==================== 页面设置快捷方式 ====================

/**
 * 快速设置带主题的页面内容
 */
fun ComponentActivity.setContentWithTheme(content: @Composable (PaddingValues) -> Unit) {
    setContent { PageTheme { content(it) } }
}

/**
 * 快速设置LazyColumn页面 - 最常用的页面类型
 */
fun ComponentActivity.setContentWithRv(content: @Composable LazyItemScope.() -> Unit) {
    setContentWithTheme { Items(PagePaddingModifier.padding(it), content = content) }
}

/**
 * 快速设置Column页面
 */
fun ComponentActivity.setContentWithScroll(content: @Composable ColumnScope.() -> Unit) {
    setContentWithTheme { ScrollPage(PagePaddingModifier.padding(it), content = content) }
}

/**
 * 设置无padding的LazyColumn页面
 */
fun ComponentActivity.setContentWithRvNoPadding(content: @Composable LazyItemScope.() -> Unit) {
    setContentWithTheme { Items(Modifier.fillMaxWidth(), content = content) }
}

// ==================== 页面容器 ====================

@Composable
fun PageTheme(content: @Composable (PaddingValues) -> Unit) {
    P1Theme {
        Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
            content(innerPadding)
        }
    }
}

@Composable
fun Items(modifier: Modifier = Modifier, content: @Composable LazyItemScope.() -> Unit) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item(content = content)
    }
}

@Composable
fun <T> RvPage(items: Array<T>, modifier: Modifier = Modifier, key: ((T) -> Any)? = null, content: @Composable LazyItemScope.(T) -> Unit) {
    RvPageIter(items, modifier, key) { _, t -> content(t) }
}

@Composable
fun <T> RvPageIter(items: Array<T>, modifier: Modifier = Modifier, key: ((T) -> Any)? = null, content: @Composable LazyItemScope.(Int, T) -> Unit) {
    LazyColumn(modifier = modifier) {
        items(items.size, key = { key?.invoke(items[it]) ?: it }) { content(it, items[it]) }
    }
}

@Composable
fun ScrollPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), content = content)
}

// ==================== 常用组合组件 ====================

/**
 * 基础按钮 - 90%场景使用
 */
@Composable
fun ClickButton(text: String, modifier: Modifier = Modifier, enable: Boolean = true, colors: ButtonColors = ButtonDefaults.buttonColors(), onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enable,
        colors = colors,
    ) {
        Text(text)
    }
}

/**
 * 状态按钮 - 显示不同状态的按钮
 */
@Composable
fun StateButton(text: String, state: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ClickButton(
        text = text, modifier = modifier, colors = ButtonDefaults.buttonColors(
            containerColor = if (state) Color.Red else MaterialTheme.colorScheme.primary
        ), onClick = onClick
    )
}

/**
 * 页面跳转按钮 - 快速跳转到其他Activity
 */
@Composable
fun JumpButton(text: String, modifier: Modifier = Modifier, clazz: KClass<out Activity>) {
    val intent = if (LocalInspectionMode.current) null else Intent(currAct, clazz.java)
    ClickButton(text, modifier) {
        intent?.let { currAct.startActivity(it) }
    }
}

@Composable
fun ClickIcon(icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick) { Icon(painter = rememberVectorPainter(icon), null, modifier = modifier) }
}

// ==================== 表单组件 ====================

/**
 * 带标签的复选框 - 常用表单组件
 */
@Composable
fun CheckBoxLabel(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onCheckedChange(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

/**
 * 单选组 - 简化表单选择
 */
@Composable
fun RadioGroup(options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    Column {
        options.forEachIndexed { index, text ->
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .clickable { onSelected(index) }
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)) {
                RadioButton(
                    selected = index == selectedIndex, onClick = { onSelected(index) })
                Text(
                    text = text, modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

// ==================== 布局工具 ====================

/**
 * 垂直间距
 */
@Composable
fun SpacerV(height: Dp = PagePadding) {
    Spacer(Modifier.height(height))
}

/**
 * 水平间距
 */
@Composable
fun SpacerH(width: Dp = PagePadding) {
    Spacer(Modifier.width(width))
}

/**
 * 分割线 + 间距的组合
 */
@Composable
fun DividerWithSpacer() {
    Column {
        HorizontalDivider()
        SpacerV()
    }
}

// ==================== 内容状态 ====================

/**
 * 空状态提示
 */
@Composable
fun EmptyMsg(isEmpty: Boolean, message: String = "暂无数据", content: @Composable () -> Unit) {
    if (isEmpty) {
        Text(
            text = message, modifier = PagePaddingModifier, textAlign = TextAlign.Center, color = Color.Gray
        )
    } else {
        content()
    }
}

// ==================== 工具函数 ====================

/**
 * 快速创建ComposeView
 */
fun createComposeView(ctx: Context, content: @Composable () -> Unit): View {
    return ComposeView(ctx).apply { setContent(content) }
}

/**
 * 移除底部padding的修饰符
 */
fun Modifier.paddingNoBottom(horizontal: Dp, top: Dp) = padding(start = horizontal, end = horizontal, top = top)

/**
 * 移除顶部padding的修饰符
 */
fun Modifier.paddingNoTop(horizontal: Dp, bottom: Dp) = padding(start = horizontal, end = horizontal, bottom = bottom)