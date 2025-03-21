package com.munch1182.lib.base

import java.nio.ByteBuffer
import java.nio.ByteOrder

inline fun <reified T> Array<out T>.toArray() = Array(size) { this[it] }

fun ByteArray.toShortArray(order: ByteOrder = ByteOrder.LITTLE_ENDIAN): ShortArray {
    val array = ShortArray(size / 2)
    ByteBuffer.wrap(this).order(order).asShortBuffer().get(array)
    return array
}

fun ShortArray.toByteArray(order: ByteOrder = ByteOrder.LITTLE_ENDIAN): ByteArray {
    val buffer = ByteBuffer.allocate(size * 2).order(order)
    this.forEach { buffer.putShort(it) }
    return buffer.array()
}