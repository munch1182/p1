package com.munch1182.lib.helper

import android.content.Context
import androidx.core.content.edit
import com.munch1182.lib.AppHelper

interface DataHelperImpl {

    suspend fun <T> put(key: String, value: T)
    suspend fun <T> get(key: String, defaultValue: T? = null): T?
}

class SPHelper(private val name: String) : DataHelperImpl {

    private val sp get() = AppHelper.getSharedPreferences(name, Context.MODE_PRIVATE)

    override suspend fun <T> put(key: String, value: T) {
        sp.edit {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("Unsupported type: $value")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> get(key: String, defaultValue: T?): T? {
        return when (defaultValue) {
            is String -> sp.getString(key, defaultValue)
            is Int -> sp.getInt(key, defaultValue)
            is Boolean -> sp.getBoolean(key, defaultValue)
            is Float -> sp.getFloat(key, defaultValue)
            is Long -> sp.getLong(key, defaultValue)
            else -> throw IllegalArgumentException("Unsupported type?: $defaultValue")
        } as T?
    }
}

