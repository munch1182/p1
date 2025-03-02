package com.munch1182.p1

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.setPadding
import com.munch1182.lib.keepScreenOn
import com.munch1182.lib.randomColor
import com.munch1182.libview.SlideMenuLayout
import com.munch1182.p1.ui.theme.P1Theme

class SlideMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        keepScreenOn()
        setContentWithBase { SlideMenu(it) }
    }
}

@Composable
fun SlideMenu(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(100) { index ->
            AndroidView(factory = {
                SlideMenuLayout(it).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    setBackgroundColor(randomColor)
                    setPadding(16.dp.value.toInt())
                    addView(ComposeView(it).apply {
                        setContent {
                            Text("item $index")
                            Text("DEL")
                        }
                    })
                }
            })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SlideMenuPreview() {
    P1Theme { SlideMenu() }
}