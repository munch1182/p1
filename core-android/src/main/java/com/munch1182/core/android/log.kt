package com.munch1182.core.android

import com.munch1182.core.common.Logger
import com.munch1182.core.common.LogLevel
import com.tencent.mars.xlog.Xlog
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 日志记录器
 *
 * 使用时需要先调用[addLogger], 传入的参数才是实际执行的对象
 */
object Log : Logger {
    private val loggers = CopyOnWriteArrayList<Logger>()

    /**
     * 添加日志记录器
     */
    fun addLogger(logger: Logger): Log {
        loggers.add(logger)
        return this
    }

    /**
     * 输出日志
     */
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        loggers.forEach { it.log(level, tag, message, throwable) }
    }
}

/**
 * 默认初始化日志：
 * 默认添加一个Debug下的
 *
 * 只能在[android.app.Application.onCreate]及其之后调用
 */
fun Log.initDefault() {
    System.loadLibrary("c++_shared")
    System.loadLibrary("marsxlog")
    if (AppHelper.isDebug) {
        addLogger(
            ConsoleLogger(
                AppHelper.isDebug, listOf(Logger::class.java.name, Log::class.java.name)
            )
        )
    }
    addLogger(
        FileLogger(
            newFile("logs").absolutePath, newCache("logs").absolutePath
        )
    )
}

/**
 * 控制台日志记录器
 *
 * 因为使用了stackTrace，建议在debug模式下使用
 */
class ConsoleLogger(
    private val enableCaller: Boolean = false, //
    private val callerClass: List<String> = emptyList(), //
    private val compatTag: Boolean = true //
) : Logger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val tag = if (compatTag) "loglog-${tag}" else tag
        val message = if (enableCaller) "${message}\t\t--${collectCaller()}" else message
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d(tag, message, throwable)
            LogLevel.INFO -> android.util.Log.i(tag, message, throwable)
            LogLevel.WARN -> android.util.Log.w(tag, message, throwable)
            LogLevel.ERROR -> android.util.Log.e(tag, message, throwable)
            LogLevel.VERBOSE -> android.util.Log.v(tag, message, throwable)
        }
    }

    private fun collectCaller(): String {
        val ele = findFirstCallClass()
        val threadName = Thread.currentThread().name
        val elsStr = ele?.let { "${it.methodName}(${it.fileName}:${it.lineNumber})" } ?: "Unknown"
        return "(${threadName})$elsStr"
    }

    private fun findFirstCallClass(): StackTraceElement? {
        val currClassName = this::class.java.name
        for (ele in Throwable("").stackTrace) {
            val eleClassName = ele.className
            if (callerClass.contains(eleClassName) || eleClassName == currClassName) continue
            return ele
        }
        return null
    }
}


/**
 * 无需关闭的写入日志文件实现, 即其生命周期跟随整个app
 */
class FileLogger(
    fileDir: String,
    cacheDir: String,
    minLevel: LogLevel = LogLevel.DEBUG,
    cacheDays: Int = 3,
    namePrefix: String = "log",
    pubkey: String = ""
) : Logger, Closeable {

    init {
        val cfg = Xlog.XLogConfig()
        cfg.level = minLevel.map2XlogLevel()
        cfg.logdir = fileDir
        cfg.cachedir = cacheDir
        cfg.nameprefix = namePrefix
        cfg.cachedays = cacheDays
        cfg.compressmode = Xlog.ZLIB_MODE
        cfg.pubkey = pubkey
        Xlog.appenderOpen(cfg)
    }

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        Xlog.log(level.map2XlogLevel(), tag, message)
    }

    override fun close() {
        Xlog().appenderClose() // jni设置不是static的, 后面重新编译再更改
    }
}

private fun LogLevel.map2XlogLevel() = when (this) {
    LogLevel.DEBUG -> Xlog.LEVEL_DEBUG
    LogLevel.INFO -> Xlog.LEVEL_INFO
    LogLevel.WARN -> Xlog.LEVEL_WARNING
    LogLevel.ERROR -> Xlog.LEVEL_ERROR
    LogLevel.VERBOSE -> Xlog.LEVEL_VERBOSE
}
