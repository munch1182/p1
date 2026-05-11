package com.munch1182.core.android

import android.os.Build
import com.munch1182.core.android.Log.addLogger
import com.munch1182.core.common.LogLevel
import com.munch1182.core.common.Logger
import com.tencent.mars.xlog.Xlog
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 日志记录器
 *
 * 使用时需要先调用[addLogger], 传入的参数才是实际执行的对象
 * 为简化调用，此方法不提供移除或关闭[Logger]的方法，如有需要，请使用自定义实现。
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
 * 默认添加一个Debug下的[ConsoleLogger]和一个随app生命周期一致的[FileLogger]
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
 * @param enableCaller 是否启用调用者文件和方法信息 (因为要在主线程遍历栈信息, 不建议非debug模式下使用)
 * @param callerClass 调用者类名列表，用于过滤不需要的调用者类
 * @param compatTag 是否启用兼容tag，默认启用，启用后tag前缀为"loglog-", 可用于统一过滤
 *
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

    /**
     * 查找第一个调用类的堆栈跟踪元素
     * 根据Android系统版本使用不同的方法来获取调用信息
     *
     * @return 返回第一个调用类的StackTraceElement，如果没有找到则返回null
     */
    private fun findFirstCallClass(): StackTraceElement? {
        val currClassName = this::class.java.name
        val skipPredicate = { className: String ->
            className == currClassName || callerClass.contains(className)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // 对于较新的Android版本，使用Java 9+的StackWalker API
            StackWalker.getInstance().walk { stream ->
                stream.dropWhile { skipPredicate(it.className) }.findFirst().orElse(null)
            }?.toStackTraceElement()
        } else {
            // 对于较旧的Android版本，使用传统的Thread.getStackTrace()
            Thread.currentThread().stackTrace.firstOrNull { !skipPredicate(it.className) }
        }
    }
}


/**
 * 无需关闭的写入日志文件实现, 即其生命周期跟随整个app
 *
 * @param fileDir 日志文件目录
 * @param cacheDir 日志缓存目录
 * @param minLevel 最小日志级别
 * @param cacheDays 缓存天数
 * @param namePrefix 日志文件前缀
 * @param pubkey 加密公钥
 */
class FileLogger(
    fileDir: String, cacheDir: String, minLevel: LogLevel = LogLevel.DEBUG, cacheDays: Int = 3, namePrefix: String = "log", pubkey: String = ""
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
