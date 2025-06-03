package com.munch1182.lib.base

class UnSupportImpl : RuntimeException("UnSupportImpl")

fun <T> withCatch(any: () -> T): T? {
    try {
        return any()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}