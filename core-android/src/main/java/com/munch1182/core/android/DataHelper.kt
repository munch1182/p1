package com.munch1182.core.android

import com.munch1182.core.common.Preferences
import com.tencent.mmkv.MMKV

object DataHelper : Preferences {

    init {
        MMKV.initialize(AppHelper)
    }

    private val mmkv = MMKV.defaultMMKV()

    override fun put(key: String, value: String) {
        mmkv.encode(key, value)
    }

    override fun put(key: String, value: Int) {
        mmkv.encode(key, value)
    }

    override fun put(key: String, value: Long) {
        mmkv.encode(key, value)
    }

    override fun put(key: String, value: Float) {
        mmkv.encode(key, value)
    }

    override fun put(key: String, value: Boolean) {
        mmkv.encode(key, value)
    }

    override fun put(key: String, value: Set<String>) {
        mmkv.encode(key, value)
    }

    override fun getString(key: String): String? {
        return if (mmkv.containsKey(key)) mmkv.decodeString(key) else null
    }

    override fun getInt(key: String): Int? {
        return if (mmkv.containsKey(key)) mmkv.decodeInt(key) else null
    }

    override fun getLong(key: String): Long? {
        return if (mmkv.containsKey(key)) mmkv.decodeLong(key) else null
    }

    override fun getFloat(key: String): Float? {
        return if (mmkv.containsKey(key)) mmkv.decodeFloat(key) else null
    }

    override fun getBoolean(key: String): Boolean? {
        return if (mmkv.containsKey(key)) mmkv.decodeBool(key) else null
    }

    override fun getStringSet(key: String): Set<String>? {
        // MMKV 的 decodeStringSet 在 key 不存在时直接返回 null，无需额外 containsKey 判断
        return mmkv.decodeStringSet(key)
    }

    override fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    override fun clear() {
        mmkv.clearAll()
    }
}