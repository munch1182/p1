package com.munch1182.lib.base

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min

// 对此页面的更改以复制即用为原则
open class Logger(val tag: String, var enable: Boolean = true) {

    constructor(any: Any, enable: Boolean = true) : this(any::class.java.simpleName, enable)

    fun logStr(str: String) {
        if (!isLogEnable) return
        val thread = Thread.currentThread()
        val trace = thread.stackTrace
        val threadName = thread.name

        Log.d(actualLogTag, "$str\t[thread($threadName)/${trace.dumpStackInfo()}]")
    }

    fun logStrSplit(str: String, size: Int = 300) {
        var index = 0
        val length = str.length
        while (index < str.length) {
            val min = min(index + size, length)
            logStr(str.substring(index, min))
            index += size
        }
    }

    fun log(vararg any: Any?) {
        if (!isLogEnable) return
        val thread = Thread.currentThread() // stack绑定位置
        val trace = thread.stackTrace
        val threadName = thread.name

        val isErr = any.size == 1 && any[0] is Throwable
        val data = if (any.size == 1) any[0] else any

        Any2StrFmt.any2Str(data).split(Any2StrFmt.LINE_SEPARATOR).forEachIndexed { index, it ->
            if (index == 0) {
                val content = "$it [thread($threadName)_${trace.dumpStackInfo()}]"
                if (isErr) Log.e(actualLogTag, content) else Log.d(actualLogTag, content)
            } else {
                if (isErr) Log.e(actualLogTag, it) else Log.d(actualLogTag, it)
            }
        }
    }

    private fun Array<StackTraceElement>.dumpStackInfo(): String {
        forEachIndexed { index, ele ->
            if (ele.className == Logger::class.java.name) {
                // 栈的位置在Thread.currentThread().stackTrace的位置而不在判断的位置
                // 必须在Logger内调用才能这样判断
                return this.getOrNull(index + 1)?.let { Any2StrFmt.any2Str(it) } ?: ""
            }
        }
        return ""
    }

    protected open val isLogEnable: Boolean
        get() = getLoggerGlobal().forceEnable || enable
    protected open val actualLogTag: String
        get() = getLoggerGlobal().prefix?.let { "${it}$$tag" } ?: tag

    protected open fun getLoggerGlobal() = LoggerGlobal
}

object LoggerGlobal {
    var prefix: String? = null
    var forceEnable: Boolean = false // 忽略其余enable
}

// Editor // Live Templates // ll => Loglog.log("${$cursor$}")
object Loglog : Logger("LogLog", true)

internal object Any2StrFmt {

    internal val LINE_SEPARATOR = System.lineSeparator() ?: "\n"

    fun any2Str(any: Any?) = when (any) {
        null -> "null"
        is Number -> any.toFmtStr()
        is Char -> any.toFmtStr()
        is Iterable<*> -> fmtIter(any)
        is Array<*> -> fmtIter(any.asIterable())
        is ByteArray -> fmtIter(any.asIterable())
        is ShortArray -> fmtIter(any.asIterable())
        is IntArray -> fmtIter(any.asIterable())
        is LongArray -> fmtIter(any.asIterable())
        is FloatArray -> fmtIter(any.asIterable())
        is DoubleArray -> fmtIter(any.asIterable())
        is CharArray -> fmtIter(any.asIterable())
        is String -> any.toFmtStr()
        is Throwable -> any.toFmtStr()
        is StackTraceElement -> any.toFmtStr()
        else -> any.toString()
    }

    private fun String.toFmtStr(): String {
        return kotlin.runCatching { JSONObject(this).toString(4) }.getOrNull() ?: kotlin.runCatching { JSONArray(this).toString(4) }.getOrNull() ?: "\"$this\""
    }

    private fun Throwable.toFmtStr(): String {
        val any = this
        val sb = StringBuilder()
        val cause = any.cause
        sb.append("EXCEPTION: ").append('[').append(any.javaClass.canonicalName).append(':').append(any.message).append(']').append(LINE_SEPARATOR)
        if (cause == null) {
            any.stackTrace.forEachIndexed { index, e ->
                if (index > 0) {
                    sb.append(LINE_SEPARATOR)
                }
                sb.append("\t\t").append(e.toFmtStr())
            }
        } else {
            sb.append("CAUSED: [").append(cause.javaClass.canonicalName).append(':').append(any.message).append(']').append(LINE_SEPARATOR)
            cause.stackTrace.forEachIndexed { index, e ->
                if (index > 0) {
                    sb.append(LINE_SEPARATOR)
                }
                sb.append("\t\t").append(e.toFmtStr())
            }
        }
        return sb.toString()
    }

    private fun StackTraceElement.toFmtStr(): String {
        return "${className.split(".").lastOrNull()}#${methodName}(${fileName}:${lineNumber})"
    }

    private fun fmtIter(iter: Iterable<*>): String = iter.joinToString(", ", "[", "]") { any2Str(it) }

    private fun Char.toFmtStr() = "\'$this\'(${this.code})"

    private fun Number.toFmtStr() = when (this) {
        is Byte -> String.format("$this(0x%02X)", this)
        is Short -> "${this}(s)"
        is Float -> "${this}F"
        is Long -> "${this}L"
        is Double -> "${this}D"
        else -> this.toString()
    }
}

fun Any.log(enable: Boolean = true) = Logger(this, enable)

fun Logger.logLife(owner: LifecycleOwner, prefix: String = "Life") {
    if (!enable) return
    owner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            logStr("${prefix}: onCreate")
        }

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            logStr("${prefix}: onStart")
        }

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            logStr("${prefix}: onResume")
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            logStr("${prefix}: onPause")
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            logStr("${prefix}: onStop")
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            logStr("${prefix}: onDestroy")
        }
    })
}

fun Logger.newLog(tag: String) = Logger("${this.tag}$$tag", enable)