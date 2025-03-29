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
import com.munch1182.lib.helper.curr
import com.munch1182.p1.ui.theme.P1Theme
import com.munch1182.p1.ui.theme.PagePadding
import kotlin.reflect.KClass

fun ComponentActivity.setContentNoContainer(content: @Composable (PaddingValues) -> Unit) {
    setContent {
        P1Theme {
            Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                content(innerPadding)
            }
        }
    }
}

fun ComponentActivity.setContentWithBase(content: @Composable LazyItemScope.() -> Unit) {
    setContentNoContainer { ip ->
        LazyColumn(
            modifier = Modifier
                .padding(ip)
                .padding(PagePadding)
                .fillMaxWidth()
        ) {
            item { content(this) }
        }
    }
}

fun ComponentActivity.setContentWithNoScroll(content: @Composable ColumnScope.() -> Unit) {
    setContentNoContainer { ip ->
        Column(
            modifier = Modifier
                .padding(ip)
                .padding(PagePadding)
                .fillMaxWidth()
        ) {
            content(this)
        }
    }
}

@Composable
fun ClickButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier = modifier) { Text(text) }
}

@Composable
fun JumpButton(text: String, modifier: Modifier = Modifier, clazz: KClass<out Activity>) {
    JumpButton(text, modifier, Intent(curr, clazz.java))
}

@Composable
fun JumpButton(text: String, modifier: Modifier = Modifier, intent: Intent) {
    ClickButton(text, modifier) { curr.startActivity(intent) }
}

@Composable
fun CheckBoxWithLabel(
    label: String, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange?.invoke(!checked) }) {
        Checkbox(checked, onCheckedChange)
        Text(label)
    }
}

@Composable
fun Split() {
    Spacer(Modifier.height(PagePadding))
}