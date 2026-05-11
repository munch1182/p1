package com.munch1182.core.android

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.munch1182.core.common.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("app")

object DataHelper : Preferences {
    private val dataStore = AppHelper.dataStore

    override suspend fun put(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun put(key: String, value: Int) {
        dataStore.edit { it[intPreferencesKey(key)] = value }
    }

    override suspend fun put(key: String, value: Long) {
        dataStore.edit { it[longPreferencesKey(key)] = value }
    }

    override suspend fun put(key: String, value: Float) {
        dataStore.edit { it[floatPreferencesKey(key)] = value }
    }

    override suspend fun put(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    override suspend fun put(key: String, value: Set<String>) {
        dataStore.edit { it[stringSetPreferencesKey(key)] = value }
    }

    override fun getString(key: String): Flow<String?> {
        return dataStore.data.map { it[stringPreferencesKey(key)] }
    }

    override fun getInt(key: String): Flow<Int?> {
        return dataStore.data.map { it[intPreferencesKey(key)] }
    }

    override fun getLong(key: String): Flow<Long?> {
        return dataStore.data.map { it[longPreferencesKey(key)] }
    }

    override fun getFloat(key: String): Flow<Float?> {
        return dataStore.data.map { it[floatPreferencesKey(key)] }
    }

    override fun getBoolean(key: String): Flow<Boolean?> {
        return dataStore.data.map { it[booleanPreferencesKey(key)] }
    }

    override fun getStringSet(key: String): Flow<Set<String>?> {
        return dataStore.data.map { it[stringSetPreferencesKey(key)] }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(key))
            preferences.remove(intPreferencesKey(key))
            preferences.remove(longPreferencesKey(key))
            preferences.remove(floatPreferencesKey(key))
            preferences.remove(booleanPreferencesKey(key))
            preferences.remove(stringSetPreferencesKey(key))
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}