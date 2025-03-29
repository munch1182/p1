package com.munch1182.lib.base

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
