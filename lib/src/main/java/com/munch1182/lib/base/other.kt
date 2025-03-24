package com.munch1182.lib.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun <T> MutableLiveData<T>.asLive(): LiveData<T> = this
fun <T> MutableStateFlow<T>.asStateFlow(): StateFlow<T> = this