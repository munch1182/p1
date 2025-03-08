package com.munch1182.lib.base

inline fun <reified T> Array<out T>.toArray() = Array(size) { this[it] }