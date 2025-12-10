package com.munch1182.lib.helper

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.munch1182.lib.AppHelper
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * 同一个文件在同一实例中只有一个对象
 */
class DataStore(val name: String = "AppData") {
    val AppHelper.data by preferencesDataStore(name)

    suspend fun <T> save(key: Key<T?>, value: T?) = save(key.key, value)

    suspend fun <T> get(key: Key<T>) = get(key.key)

    private suspend fun <T> save(key: Preferences.Key<T?>, value: T?) {
        AppHelper.data.edit { it[key] = value }
    }

    private suspend fun <T> get(key: Preferences.Key<T>): T? {
        return AppHelper.data.data.map { it[key] }.firstOrNull()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> generateKey(key: String): Key<T> {
            return Key(
                when (T::class) {
                    Boolean::class -> booleanPreferencesKey(key)
                    Int::class -> intPreferencesKey(key)
                    Long::class -> longPreferencesKey(key)
                    Float::class -> floatPreferencesKey(key)
                    Double::class -> doublePreferencesKey(key)
                    String::class -> stringPreferencesKey(key)
                    else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
                } as Preferences.Key<T>
            )
        }
    }

    // 给不引用DataStore的库使用
    class Key<T>(val key: Preferences.Key<T>)
}
