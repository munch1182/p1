package com.munch1182.lib.base

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


open class Logger(private val tag: String) {
    fun log(vararg any: Any?) {
        val thread = Thread.currentThread()
        val trace = thread.stackTrace
        val threadName = thread.name

        val anyValue = if (any.size == 1) any[0] else any
        FMT.any2Str(anyValue).split(FMT.LINE_SEPARATOR).forEachIndexed { index, s ->
            if (index == 0) {
                Log.d(tag, "$s\t[thread($threadName)/${trace.dumpStackInfo()}]")
            } else {
                Log.d(tag, s)
            }
        }
    }

    private fun Array<StackTraceElement>.dumpStackInfo(): String {
        forEachIndexed { index, ele ->
            if (ele.className == Logger::class.java.name) {
                // 栈的位置在Thread.currentThread().stackTrace的位置而不在判断的位置
                // 必须在Logger内调用才能这样判断
                return this.getOrNull(index + 1)?.let { FMT.fmtStace(it) } ?: ""
            }
        }
        return ""
    }

    private fun print(any: Any?) {
        val msg = FMT.any2Str(any)
        Log.d(tag, msg)
    }

    internal object FMT {
        val LINE_SEPARATOR = System.lineSeparator() ?: ""

        fun any2Str(any: Any?): String {
            return when (any) {
                null -> "null"
                is Byte -> any.toHexStr()
                is Double -> "${any}D"
                is Float -> "${any}F"
                is Long -> "${any}L"
                is Char -> "'${any}'"
                is Number -> any.toString()
                is String -> fmtStr(any)
                is Throwable -> fmtThrow(any)
                is Iterator<*> -> fmtIter(any)
                is Iterable<*> -> fmtIter(any.iterator())
                is Array<*> -> fmtIter(any.iterator())
                is IntArray -> fmtIter(any.iterator())
                is LongArray -> fmtIter(any.iterator())
                is FloatArray -> fmtIter(any.iterator())
                is DoubleArray -> fmtIter(any.iterator())
                is CharArray -> fmtIter(any.iterator())
                is BooleanArray -> fmtIter(any.iterator())
                is ShortArray -> fmtIter(any.iterator())
                else -> any.toString()
            }
        }

        private fun fmtIter(iter: Iterator<Any?>): String {
            return iter.asSequence().joinToString(", ", "[", "]") { any2Str(it) }
        }

        fun fmtStace(e: StackTraceElement): String {
            return "${
                e.className.split(".").lastOrNull()
            }#${e.methodName}(${e.fileName}:${e.lineNumber})"
        }

        private fun fmtStr(str: String): String {
            return if (canFormatJson(str)) fmtJson(str) else "\"$str\""
        }

        private fun fmtJson(str: String): String {
            return try {
                when {
                    str.startsWith("{") -> JSONObject(str).toString(4)
                    str.startsWith("[") -> JSONArray(str).toString(4)
                    else -> throw IllegalStateException("cannot format json start with ${str[0]}")
                }
            } catch (e: JSONException) {
                "cannot format json : ${e.message}"
            } catch (e: IllegalStateException) {
                e.message!!
            }.let { "[JSON] ↓↓↓ $LINE_SEPARATOR$it" }
        }

        private fun canFormatJson(str: String): Boolean {
            return try {
                JSONObject(str)
                true
            } catch (_: Exception) {
                false
            }
        }

        private fun fmtThrow(any: Throwable): String {
            val sb = StringBuilder()
            val cause = any.cause
            sb.append("EXCEPTION: ")
                .append('[')
                .append(any.javaClass.canonicalName)
                .append(':')
                .append(any.message)
                .append(']')
                .append(LINE_SEPARATOR)
            if (cause == null) {
                any.stackTrace.forEachIndexed { index, e ->
                    if (index > 0) {
                        sb.append(LINE_SEPARATOR)
                    }
                    sb.append("\t\t").append(fmtStace(e))
                }
            } else {
                sb.append("CAUSED: [")
                    .append(cause.javaClass.canonicalName)
                    .append(':')
                    .append(any.message)
                    .append(']')
                    .append(LINE_SEPARATOR)
                cause.stackTrace.forEachIndexed { index, e ->
                    if (index > 0) {
                        sb.append(LINE_SEPARATOR)
                    }
                    sb.append("\t\t").append(fmtStace(e))
                }
            }
            return sb.toString()
        }
    }
}

// Editor // Live Templates // ll => Loglog.log("${$cursor$}")
object Loglog : Logger("LogLog")
