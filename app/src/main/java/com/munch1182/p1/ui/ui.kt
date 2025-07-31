package com.munch1182.p1.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    P1Theme { Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding -> content(innerPadding) } }
}

@Composable
fun RvPage(modifier: Modifier = Modifier, content: @Composable LazyItemScope.() -> Unit) {
    LazyColumn(modifier = modifier.fillMaxWidth()) { item(content = content) }
}

@Composable
fun ScrollPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), content = content)
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

fun ComponentActivity.setContentWithRv(modifier: Modifier = PagePaddingModifier, content: @Composable LazyItemScope.() -> Unit) {
    setContentWithTheme { RvPage(if (NoApplyWindowPadding.isNotPadding(modifier)) modifier else modifier.padding(it), content = content) }
}

fun ComponentActivity.setContentWithScroll(modifier: Modifier = PagePaddingModifier, content: @Composable ColumnScope.() -> Unit) {
    setContentWithTheme { ScrollPage(if (NoApplyWindowPadding.isNotPadding(modifier)) modifier else modifier.padding(it), content = content) }
}

@Composable
fun ClickButton(text: String, modifier: Modifier = Modifier, enable: Boolean = true, colors: ButtonColors = ButtonDefaults.buttonColors(), textColor: Color = Color.Unspecified, onClick: () -> Unit) {
    Button(onClick, modifier = modifier, enabled = enable, colors = colors) { Text(text, color = textColor) }
}

@Composable
fun StateButton(text: String, state: Boolean, modifier: Modifier = Modifier, enable: Boolean = true, textColor: Color = Color.Unspecified, onClick: () -> Unit) {
    ClickButton(
        text, modifier, enable = enable, colors = ButtonDefaults.buttonColors(if (state) Color.Red else Color.Unspecified),
        textColor = textColor, onClick = onClick
    )
}

@Composable
fun JumpButton(text: String, modifier: Modifier = Modifier, clazz: KClass<out Activity>) {
    JumpButton(text, modifier, if (LocalInspectionMode.current) null else Intent(currAct, clazz.java))
}

@Composable
fun JumpButton(text: String, modifier: Modifier = Modifier, intent: Intent? = null) {
    val isPreMode = LocalInspectionMode.current
    ClickButton(text, modifier) { if (!isPreMode) currAct.startActivity(intent) }
}

@Composable
fun CheckBoxLabel(label: String, checked: Boolean, enable: Boolean = true, onCheckedChange: ((Boolean) -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(enabled = enable) { onCheckedChange?.invoke(!checked) }) {
        Checkbox(checked, onCheckedChange = onCheckedChange, enabled = enable)
        Text(label)
    }
}

@Composable
fun RowScope.Split() = SplitV()

@Composable
fun SplitV() {
    Spacer(Modifier.height(PagePadding))
}

@Composable
fun ColumnScope.Split() {
    Spacer(Modifier.width(PagePadding))
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
fun DownUpArrow(downOrUp: Boolean, modifier: Modifier = Modifier, onClick: (Boolean) -> Unit) {
    CircleBG({ onClick(downOrUp) }) {
        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = modifier.rotate(if (downOrUp) 180f else 0f))
    }
}

@Composable
fun EmptyMsg(isEmpty: Boolean, msg: String, content: @Composable () -> Unit) {
    if (isEmpty) Text(msg, modifier = PagePaddingModifier, textAlign = TextAlign.Center) else content()
}

fun ComposeView(ctx: Context, content: @Composable () -> Unit): View {
    return ComposeView(ctx).apply { setContent(content) }
}

@Composable
fun RadioGroup(names: Array<String>, selectIndex: Int, modifier: Modifier = Modifier, onChose: (Int) -> Unit) {
    var select by remember { mutableIntStateOf(selectIndex) }
    Column {
        names.forEachIndexed { index, s ->
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .clickable(onClick = { select = index; onChose(index) })
                    .padding(end = 16.dp)
            ) {
                RadioButton(index == select, onClick = { select = index; onChose(index) }, modifier = modifier)
                Text(s)
            }
        }

    }
}
