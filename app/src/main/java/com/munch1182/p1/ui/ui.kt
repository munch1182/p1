package com.munch1182.p1.ui

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import com.munch1182.lib.helper.currAct
import com.munch1182.p1.ui.theme.FontManySize
import com.munch1182.p1.ui.theme.P1Theme
import com.munch1182.p1.ui.theme.PagePadding
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

fun ComponentActivity.setContentWithRv(modifier: Modifier = Modifier.padding(PagePadding), content: @Composable LazyItemScope.() -> Unit) {
    setContentWithTheme { ip -> RvPage(modifier.padding(ip)) { content(this) } }
}

fun ComponentActivity.setContentWithScroll(modifier: Modifier = Modifier.padding(PagePadding), content: @Composable ColumnScope.() -> Unit) {
    setContentWithTheme { ip -> ScrollPage(modifier.padding(ip)) { content(this) } }
}

@Composable
fun ClickButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier = modifier) { Text(text) }
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