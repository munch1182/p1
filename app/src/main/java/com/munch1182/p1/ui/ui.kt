package com.munch1182.p1.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.munch1182.lib.helper.currAct
import com.munch1182.p1.ui.theme.FontManySize
import com.munch1182.p1.ui.theme.P1Theme
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingModifier
import kotlin.reflect.KClass

fun ComponentActivity.setContentWithTheme(content: @Composable (PaddingValues) -> Unit) {
    setContent { PageTheme { content(it) } }
}

@Composable
fun PageTheme(content: @Composable (PaddingValues) -> Unit) {
    P1Theme {
        Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding -> content(innerPadding) }
    }
}

@Composable
fun RvPage(modifier: Modifier = Modifier, content: @Composable LazyItemScope.() -> Unit) {
    LazyColumn(modifier = modifier.fillMaxWidth()) { item { content(this) } }
}

@Composable
fun ScrollPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) { content(this) }
}

fun Modifier.noApplyWindowPadding() = this then NoApplyWindowPadding(true)

private class NoApplyWindow(var isNo: Boolean = false) : Modifier.Node()

private class NoApplyWindowPadding(val isNo: Boolean = false) : ModifierNodeElement<NoApplyWindow>() {

    companion object {
        fun isNotPadding(modifier: Modifier) = modifier.any { it is NoApplyWindowPadding && it.isNo }
    }

    override fun InspectorInfo.inspectableProperties() {
    }

    override fun create(): NoApplyWindow = NoApplyWindow(isNo)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoApplyWindowPadding) return false
        return isNo == (other).isNo
    }

    override fun hashCode() = isNo.hashCode()
    override fun update(node: NoApplyWindow) {
        node.isNo = isNo
    }
}

fun ComponentActivity.setContentWithRv(modifier: Modifier = Modifier.padding(PagePadding), content: @Composable LazyItemScope.() -> Unit) {
    setContentWithTheme { ip ->
        RvPage(if (NoApplyWindowPadding.isNotPadding(modifier)) modifier else modifier.padding(ip)) { content(this) }
    }
}

fun ComponentActivity.setContentWithScroll(modifier: Modifier = Modifier.padding(PagePadding), content: @Composable ColumnScope.() -> Unit) {
    setContentWithTheme { ip -> ScrollPage(if (NoApplyWindowPadding.isNotPadding(modifier)) modifier else modifier.padding(ip)) { content(this) } }
}

@Composable
fun ClickButton(text: String, modifier: Modifier = Modifier, colors: ButtonColors = ButtonDefaults.buttonColors(), onClick: () -> Unit) {
    Button(onClick, modifier = modifier, colors = colors) { Text(text) }
}

@Composable
fun JumpButton(text: String, modifier: Modifier = Modifier, clazz: KClass<out Activity>) {
    JumpButton(text, modifier, if (LocalInspectionMode.current) null else Intent(currAct, clazz.java))
}

@Composable
fun JumpButton(text: String, modifier: Modifier = Modifier, intent: Intent? = null) {
    val isPreMode = LocalInspectionMode.current
    ClickButton(text, modifier) {
        if (!isPreMode) currAct.startActivity(intent)
    }
}

@Composable
fun CheckBoxWithLabel(
    label: String, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onCheckedChange?.invoke(!checked) }) {
        Checkbox(checked, onCheckedChange)
        Text(label)
    }
}

@Composable
fun Split() {
    Spacer(Modifier.height(PagePadding))
}

@Composable
fun DescText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = FontManySize, color = Color.Gray)
}

@Composable
fun <T> Rv(list: Array<T>, modifier: Modifier = Modifier, key: ((T) -> Any)? = null, item: @Composable LazyItemScope.(index: T) -> Unit) {
    LazyColumn(modifier) {
        items(list.size, key = { key?.invoke(list[it]) ?: it }) {
            item(list[it])
            HorizontalDivider()
        }
    }
}

@Composable
fun StateButton(text: String, state: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ClickButton(text, modifier, colors = ButtonDefaults.buttonColors(if (state) Color.Red else Color.Unspecified), onClick)
}

@Composable
fun DownUpArrow(downOrUp: Boolean, modifier: Modifier = Modifier.size(24.dp), onClick: (Boolean) -> Unit) {
    CircleBG({ onClick(downOrUp) }) {
        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = modifier.rotate(if (downOrUp) 180f else 0f))
    }
}

fun ComposeView(ctx: Context, content: @Composable () -> Unit): ComposeView {
    return ComposeView(ctx).apply { setContent(content) }
}

fun ComposeListView(ctx: Context, size: Int, modifier: Modifier = Modifier, key: ((index: Int) -> Any)? = null, content: @Composable (Int) -> Unit): ComposeView {
    return ComposeView(ctx).apply {
        setContent {
            LazyColumn(modifier) { items(size, key) { content.invoke(it) } }
        }
    }
}

// onClick要交由CircleBG
// 如果content没有透明背景，需要增加边距
@Composable
fun CircleBG(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(CircleShape) //先clip后onClick
            .clickable(onClick = onClick), content = content
    )
}

@Composable
fun EmptyMsg(isEmpty: Boolean, msg: String, content: @Composable () -> Unit) {
    if (isEmpty) Text(msg, modifier = PagePaddingModifier, textAlign = TextAlign.Center) else content()
}