package com.munch1182.p1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.munch1182.p1.ui.theme.P1Theme

class ResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Result() }
    }
}

@Composable
fun Result() {
    
}

@Preview
@Composable
fun ResultPreview() {
    P1Theme { Result() }
}