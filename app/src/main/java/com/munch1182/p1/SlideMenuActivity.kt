package com.munch1182.p1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.munch1182.lib.keepScreenOn
import com.munch1182.lib.view.SlideMenuLayout
import com.munch1182.p1.ui.theme.P1Theme

class SlideMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepScreenOn()
        setContentWithBase { SlideMenu() }
    }
}

@Composable
fun SlideMenu() {
    LazyColumn {
        items(25) {
            SlideMenuItem(modifier = Modifier.padding(32.dp, 8.dp)) {
                Text("123", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun SlideMenuItem(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    return AndroidView(modifier = modifier, factory = {
        SlideMenuLayout(it).apply {
            addView(ComposeView(it).apply { setContent(content) })
        }
    })
}

@Preview(showBackground = true)
@Composable
fun SlideMenuPreview() {
    P1Theme {
        SlideMenu()
    }
}