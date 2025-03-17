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
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.munch1182.lib.DefaultActivityLifecycleCallbacks
import com.munch1182.lib.base.keepScreenOn
import com.munch1182.lib.helper.ActivityCurrHelper
import com.munch1182.p1.ui.theme.P1Theme

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        ActivityCurrHelper.register()
        registerActivityLifecycleCallbacks(object : DefaultActivityLifecycleCallbacks() {
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
    setContentNoContainer(parent) { modifier ->
        Column(modifier = modifier.padding(16.dp)) { content() }
    }
}


fun ComponentActivity.setContentNoContainer(
    parent: CompositionContext? = null,
    content: @Composable (Modifier) -> Unit
) {
    setContent(parent) {
        P1Theme {
            Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                content(Modifier.padding(innerPadding))
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> LiveData<T>.observeAsStateNotNull(): State<T> = observeAsState() as State<T>