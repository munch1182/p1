package com.munch1182.p1

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.munch1182.lib.ActivityLifecycleSimpleCallbacks
import com.munch1182.lib.keepScreenOn
import com.munch1182.p1.ui.theme.P1Theme

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleSimpleCallbacks() {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                super.onActivityCreated(activity, savedInstanceState)
                activity.keepScreenOn()
                if (activity is ComponentActivity) activity.enableEdgeToEdge()
            }
        })
    }
}


fun ComponentActivity.setContentWithBase(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit
) {
    setContent(parent) {
        P1Theme {
            Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                Column(modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)) {
                    content()
                }
            }
        }
    }
}