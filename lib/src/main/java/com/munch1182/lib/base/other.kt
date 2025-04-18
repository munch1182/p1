package com.munch1182.lib.base

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun <T> MutableLiveData<T>.asLive(): LiveData<T> = this
fun <T> MutableStateFlow<T>.asStateFlow(): StateFlow<T> = this

fun Lifecycle.onDestroyed(onDestroy: (LifecycleOwner) -> Unit) {
    addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            onDestroy(owner)
        }
    })
}

fun Lifecycle.onCreated(onCreate: (LifecycleOwner) -> Unit) {
    addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            onCreate(owner)
        }
    })
}

/**
 * onCreate: true/onDestroy: false
 */
fun Lifecycle.onLife(isCreateOrDestroy: (Boolean) -> Unit) {
    addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            isCreateOrDestroy.invoke(true)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            isCreateOrDestroy.invoke(false)
        }
    })
}

/**
 * onResume: true/onPause: false
 */
fun Lifecycle.onShow(isShowOrHidden: (Boolean) -> Unit) {
    addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            isShowOrHidden.invoke(true)
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            isShowOrHidden.invoke(false)
        }
    })
}

@Suppress("DEPRECATION")
inline fun <reified T> Bundle.getParcelableCompat(key: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelable(key, T::class.java) else getParcelable(key)
