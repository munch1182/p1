package com.munch1182.lib.base

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * 数值类型二进制字符串转换工具
 */
interface BinaryRepresentable

/**
 * 将数值类型转换为二进制字符串表示
 *
 * 支持类型：Byte, Short, Int, Long, Float, Double, Char
 * Float 和 Double 遵循 IEEE 754 标准
 *
 * @param format 是否格式化（4位分组）
 * @param fillZero 是否补全前导零
 */
fun Number.toBinaryStr(format: Boolean = true, fillZero: Boolean = false): String {
    val (binaryStr, totalBits) = when (this) {
        is Byte -> Integer.toBinaryString(toInt()) to Byte.SIZE_BITS
        is Short -> Integer.toBinaryString(toInt()) to Short.SIZE_BITS
        is Int -> Integer.toBinaryString(this) to Int.SIZE_BITS
        is Long -> java.lang.Long.toBinaryString(this) to Long.SIZE_BITS
        is Float -> java.lang.Float.floatToIntBits(this).toBinaryStr(false, false) to Float.SIZE_BITS
        is Double -> java.lang.Double.doubleToLongBits(this).toBinaryStr(false, false) to Double.SIZE_BITS
        else -> throw UnsupportedOperationException("Unsupported type: ${this::class.java}")
    }

    return if (format) binaryStr.formatAsBinaryGroups(totalBits, fillZero) else binaryStr
}

fun Char.toBinaryStr(format: Boolean = true, fillZero: Boolean = false): String {
    val binaryStr = code.toString(2)
    return if (format) binaryStr.formatAsBinaryGroups(Char.SIZE_BITS, fillZero) else binaryStr
}

/**
 * 格式化二进制字符串为4位一组
 */
private fun String.formatAsBinaryGroups(totalBits: Int, fillZero: Boolean): String {
    val targetLength = if (fillZero) totalBits else length + (4 - length % 4) % 4
    val padded = padStart(targetLength, '0')

    return padded.chunked(4).joinToString(" ")
}

/**
 * 浮点数精度控制扩展
 */
fun Float.toString(decimals: Int = 2): String = "%.${decimals}f".format(this)

fun Float.keep(decimals: Int): Float {
    val factor = 10.0f.pow(decimals)
    return (this * factor).roundToInt() / factor
}

fun Double.keep(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return (this * factor).roundToLong() / factor
}

/**
 * 将整数类型的Number转换为十六进制字符串
 * 支持负数的补码表示
 * 按字节单位补齐，且补齐不受trimLeadingZeros影响
 */
fun Number.toHexStr(includePrefix: Boolean = true, trimLeadingZeros: Boolean = true): String {
    // 根据不同类型获取对应的补码值和字节长度
    val (value, bytes) = when (this) {
        is Byte -> Pair(this.toInt() and 0xFF, 1)
        is Short -> Pair(this.toInt() and 0xFFFF, 2)
        is Int -> Pair((this.toLong() and 0xFFFFFFFFL).toLong(), 4)
        is Long -> Pair(this, 8)
        else -> throw IllegalArgumentException("仅支持整数类型（Byte、Short、Int、Long）")
    }

    // 计算总位数（字节数 * 2，因为每个字节2个十六进制字符）
    val totalDigits = bytes * 2

    // 生成完整十六进制字符串（按字节补齐）
    val fullHex = "%0${totalDigits}X".format(value)

    // 处理前导零的移除
    val finalHex = if (trimLeadingZeros) {
        // 计算要保留的最小位数（至少保留1位，最多保留totalDigits位）
        val minDigits = when (bytes) {
            1 -> 2  // Byte至少显示2位
            2 -> 4  // Short至少显示4位
            4 -> 8  // Int至少显示8位
            8 -> 16 // Long至少显示16位
            else -> totalDigits
        }

        // 找到第一个非零的位置，但不能小于最小位数
        val firstNonZero = fullHex.indexOfFirst { it != '0' }
        val keepFrom = if (firstNonZero == -1) {
            // 全为零，至少保留minDigits位
            totalDigits - minDigits
        } else {
            // 确保保留足够的位数
            min(firstNonZero, totalDigits - minDigits)
        }

        fullHex.substring(keepFrom)
    } else {
        fullHex
    }

    return if (includePrefix) "0x$finalHex" else finalHex
}

fun ByteArray.toHexStr(includePrefix: Boolean = false, trimLeadingZeros: Boolean = true): String = joinToString("") { it.toHexStr(includePrefix, trimLeadingZeros) }

fun ByteArray.toHexStrCompact(visibleStart: Int = 8, visibleEnd: Int = 4): String {
    return if (size > visibleStart + visibleEnd) {
        val startHex = copyOfRange(0, visibleStart).toHexStr()
        val endHex = copyOfRange(size - visibleEnd, size).toHexStr()
        "$startHex...$endHex ($size bytes)"
    } else {
        toHexStr()
    }
}

/**
 * 数值类型字节数组转换
 */
fun Number.toByteArray(bigEndian: Boolean = true): ByteArray = when (this) {
    is Long -> ByteArray(Long.SIZE_BYTES) { index ->
        val shift = if (bigEndian) (7 - index) else index
        (this ushr (shift * 8) and 0xFF).toByte()
    }

    is Int -> toByteArray(Int.SIZE_BYTES, bigEndian)
    is Float -> java.lang.Float.floatToIntBits(this).toByteArray(bigEndian)
    is Double -> java.lang.Double.doubleToLongBits(this).toByteArray(bigEndian)
    else -> throw IllegalArgumentException("Unsupported type: ${this::class.java}")
}

private fun Int.toByteArray(size: Int, bigEndian: Boolean): ByteArray = ByteArray(size) { index ->
    val shift = if (bigEndian) (size - 1 - index) else index
    (this ushr (shift * 8) and 0xFF).toByte()
}

/**
 * 字符和字符串字节数组转换
 */
fun Char.toByteArray(bigEndian: Boolean = true): ByteArray = code.toByteArray(Char.SIZE_BYTES, bigEndian)

fun String.toByteArray(charset: Charset = StandardCharsets.UTF_8): ByteArray = toByteArray(charset)

/**
 * 字节数组数值类型解析
 */
fun ByteArray.getInt(start: Int = 0, bigEndian: Boolean = true): Int {
    require(start + Int.SIZE_BYTES <= size) { "Insufficient bytes for Int" }
    return if (bigEndian) {
        (this[start].toInt() and 0xFF shl 24) or (this[start + 1].toInt() and 0xFF shl 16) or (this[start + 2].toInt() and 0xFF shl 8) or (this[start + 3].toInt() and 0xFF)
    } else {
        (this[start].toInt() and 0xFF) or (this[start + 1].toInt() and 0xFF shl 8) or (this[start + 2].toInt() and 0xFF shl 16) or (this[start + 3].toInt() and 0xFF shl 24)
    }
}

fun ByteArray.getShort(start: Int = 0, bigEndian: Boolean = true): Short = getInt(start, 2, bigEndian).toShort()

fun ByteArray.getChar(start: Int = 0, bigEndian: Boolean = true): Char = getInt(start, 2, bigEndian).toChar()

fun ByteArray.getLong(start: Int = 0, bigEndian: Boolean = true): Long {
    require(start + Long.SIZE_BYTES <= size) { "Insufficient bytes for Long" }
    return if (bigEndian) {
        (this[start].toLong() and 0xFF shl 56) or (this[start + 1].toLong() and 0xFF shl 48) or (this[start + 2].toLong() and 0xFF shl 40) or (this[start + 3].toLong() and 0xFF shl 32) or (this[start + 4].toLong() and 0xFF shl 24) or (this[start + 5].toLong() and 0xFF shl 16) or (this[start + 6].toLong() and 0xFF shl 8) or (this[start + 7].toLong() and 0xFF)
    } else {
        (this[start].toLong() and 0xFF) or (this[start + 1].toLong() and 0xFF shl 8) or (this[start + 2].toLong() and 0xFF shl 16) or (this[start + 3].toLong() and 0xFF shl 24) or (this[start + 4].toLong() and 0xFF shl 32) or (this[start + 5].toLong() and 0xFF shl 40) or (this[start + 6].toLong() and 0xFF shl 48) or (this[start + 7].toLong() and 0xFF shl 56)
    }
}

private fun ByteArray.getInt(start: Int, byteCount: Int, bigEndian: Boolean): Int {
    require(start + byteCount <= size) { "Insufficient bytes" }
    var result = 0
    repeat(byteCount) { i ->
        val byteIndex = if (bigEndian) start + byteCount - 1 - i else start + i
        result = result or ((this[byteIndex].toInt() and 0xFF) shl (i * 8))
    }
    return result
}

fun ByteArray.getFloat(start: Int = 0, bigEndian: Boolean = true): Float = java.lang.Float.intBitsToFloat(getInt(start, bigEndian))

fun ByteArray.getDouble(start: Int = 0, bigEndian: Boolean = true): Double = java.lang.Double.longBitsToDouble(getLong(start, bigEndian))

/**
 * 字节数组字符串解析
 */
fun ByteArray.getString(
    start: Int = 0, length: Int = size - start, charset: Charset = StandardCharsets.UTF_8
): String {
    require(start >= 0 && length >= 0) { "Invalid range: start=$start, length=$length" }
    require(start + length <= size) { "Range exceeds array size" }
    return String(this, start, length, charset)
}

/**
 * 数组分割工具
 */
inline fun <reified T> T.splitArray(chunkSize: Int): List<T> where T : Any {
    val arraySize = when (this) {
        is ByteArray -> size
        is IntArray -> size
        is LongArray -> size
        is CharArray -> size
        is DoubleArray -> size
        is FloatArray -> size
        else -> throw IllegalArgumentException("Unsupported array type")
    }

    require(chunkSize > 0) { "Chunk size must be positive" }

    return List((arraySize + chunkSize - 1) / chunkSize) { chunkIndex ->
        val start = chunkIndex * chunkSize
        val end = minOf(start + chunkSize, arraySize)
        copyArrayRange(start, end)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> T.copyArrayRange(start: Int, end: Int): T where T : Any {
    return when (this) {
        is ByteArray -> copyOfRange(start, end) as T
        is IntArray -> copyOfRange(start, end) as T
        is LongArray -> copyOfRange(start, end) as T
        is CharArray -> copyOfRange(start, end) as T
        is DoubleArray -> copyOfRange(start, end) as T
        is FloatArray -> copyOfRange(start, end) as T
        else -> throw IllegalArgumentException("Unsupported array type")
    }
}

/**
 * 数组子集工具
 */
inline fun <reified T> T.subArray(fromIndex: Int, toIndex: Int): T where T : Any {
    require(fromIndex >= 0 && fromIndex <= toIndex) {
        "Invalid range: fromIndex=$fromIndex, toIndex=$toIndex"
    }
    return copyArrayRange(fromIndex, toIndex)
}

fun Long.formatFileSize(): String {
    val units = listOf("B", "KB", "MB", "GB")
    var size = toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "%.1f ${units[unitIndex]}".format(size)
}