package com.munch1182.core.common

/**
 * 跨平台键值对存储接口，用于保存应用配置、用户偏好等轻量级数据。
 *
 * 该接口采用**同步方法**设计，因为正常设计读写耗时在微秒到亚毫秒级，在主线程调用不会造成可感知的卡顿。
 *
 * ## 注意事项
 * - **不适合存储大量数据或频繁的大对象**：这种场景建议使用文件、数据库等更专业的方案。
 */
interface Preferences {
    // 存储方法
    fun put(key: String, value: String)
    fun put(key: String, value: Int)
    fun put(key: String, value: Long)
    fun put(key: String, value: Float)
    fun put(key: String, value: Boolean)
    fun put(key: String, value: Set<String>)

    // 读取方法（返回可空，表示键不存在）
    fun getString(key: String): String?
    fun getInt(key: String): Int?
    fun getLong(key: String): Long?
    fun getFloat(key: String): Float?
    fun getBoolean(key: String): Boolean?
    fun getStringSet(key: String): Set<String>?

    // 删除
    fun remove(key: String)
    fun clear()
}

// ========== 扩展函数：提供带默认值的非空读取 ==========

fun Preferences.get(key: String, default: String): String = getString(key) ?: default

fun Preferences.get(key: String, default: Int): Int = getInt(key) ?: default

fun Preferences.get(key: String, default: Long): Long = getLong(key) ?: default

fun Preferences.get(key: String, default: Float): Float = getFloat(key) ?: default

fun Preferences.get(key: String, default: Boolean): Boolean = getBoolean(key) ?: default

fun Preferences.get(key: String, default: Set<String>): Set<String> = getStringSet(key) ?: default