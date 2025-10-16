package com.munch1182.p1.base

import com.munch1182.lib.helper.DataStore

object DataHelper {
    val ds = DataStore()

    sealed class DataHelperImpl<T>(val key: String) {
        suspend inline fun <reified T> get(): T? = ds.get(DataStore.generateKey(key))
        suspend inline fun <reified T> save(value: T?) = ds.save(DataStore.generateKey(key), value)
    }

    object StartIndex : DataHelperImpl<Int>("start_index")
}

