package com.munch1182.lib.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 跨平台键值对存储接口，用于保存应用配置、用户偏好等轻量级数据。
 */
interface Preferences {
    // 存储方法
    suspend fun put(key: String, value: String)
    suspend fun put(key: String, value: Int)
    suspend fun put(key: String, value: Long)
    suspend fun put(key: String, value: Float)
    suspend fun put(key: String, value: Boolean)
    suspend fun put(key: String, value: Set<String>)

    // 读取方法（返回可空，表示键不存在）
    fun getString(key: String): Flow<String?>
    fun getInt(key: String): Flow<Int?>
    fun getLong(key: String): Flow<Long?>
    fun getFloat(key: String): Flow<Float?>
    fun getBoolean(key: String): Flow<Boolean?>
    fun getStringSet(key: String): Flow<Set<String>?>

    // 删除
    suspend fun remove(key: String)
    suspend fun clear()
}


fun Preferences.get(key: String, default: String): Flow<String> = getString(key).map { it ?: default }

fun Preferences.get(key: String, default: Int): Flow<Int> = getInt(key).map { it ?: default }

fun Preferences.get(key: String, default: Long): Flow<Long> = getLong(key).map { it ?: default }

fun Preferences.get(key: String, default: Float): Flow<Float> = getFloat(key).map { it ?: default }

fun Preferences.get(key: String, default: Boolean): Flow<Boolean> = getBoolean(key).map { it ?: default }

fun Preferences.get(key: String, default: Set<String>): Flow<Set<String>> = getStringSet(key).map { it ?: default }