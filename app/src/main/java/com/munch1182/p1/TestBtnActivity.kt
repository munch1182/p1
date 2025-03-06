package com.munch1182.p1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class TestBtnActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentWithBase { TestBtn() }
    }

    @Composable
    fun TestBtn() {
        TestBtnView("测试1") {
        }
        TestBtnView("测试2") {

        }
        TestBtnView("测试3") {
        }
    }

    @Composable
    fun TestBtnView(text: String, onClick: () -> Unit) {
        Button(onClick) { Text(text) }
    }
}
