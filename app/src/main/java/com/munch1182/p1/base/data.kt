package com.munch1182.p1.base

import com.munch1182.android.lib.helper.DataStore
import com.munch1182.p1.mainScreens

object DataHelper {
    val ds = DataStore()

    sealed class DataHelperImpl<T>(val key: String) {
        suspend inline fun <reified T> get(): T? = ds.get(DataStore.generateKey(key))
        suspend inline fun <reified T> save(value: T?) = ds.save(DataStore.generateKey(key), value)
    }

    object StartIndex : DataHelperImpl<Int>("start_index") {
        suspend fun start() = get<Int>()?.takeIf { it != 0 }?.let { mainScreens.getOrNull(it)?.first }
    }

    object Translate {
        object Auth : DataHelperImpl<String>("xf_auth_file")
        object WorkDir : DataHelperImpl<String>("xf_work_dir")
    }
}

