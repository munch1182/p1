package com.munch1182.lib.base

class UnSupportImpl : RuntimeException("UnSupportImpl")

fun withCatch(any: () -> Unit) {
    try {
        any()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}