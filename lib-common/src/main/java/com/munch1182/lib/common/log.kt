package com.munch1182.lib.common

interface Logger {
    /**
     * 指定[LogLevel]并输入日志
     */
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)

    /**
     * 使用[LogLevel.VERBOSE]
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.VERBOSE, tag, message, throwable)

    /**
     * 使用[LogLevel.DEBUG]
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.DEBUG, tag, message, throwable)

    /**
     * 使用[LogLevel.INFO]
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.INFO, tag, message, throwable)

    /**
     * 使用[LogLevel.WARN]
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.WARN, tag, message, throwable)

    /**
     * 使用[LogLevel.ERROR]
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, tag, message, throwable)
}

/**
 * 日志层级，可直接进行比较顺序
 */
enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

class TaggedLogger(
    private val delegate: Logger,
    private val fixedTag: String
) : Logger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        delegate.log(level, fixedTag, message, throwable)
    }

    fun log(message: String, throwable: Throwable? = null) {
        delegate.log(LogLevel.DEBUG, fixedTag, message, throwable)
    }

    fun d(message: String, throwable: Throwable? = null) = delegate.d(fixedTag, message, throwable)
    fun i(message: String, throwable: Throwable? = null) = delegate.i(fixedTag, message, throwable)
    fun w(message: String, throwable: Throwable? = null) = delegate.w(fixedTag, message, throwable)
    fun e(message: String, throwable: Throwable? = null) = delegate.e(fixedTag, message, throwable)
    fun v(message: String, throwable: Throwable? = null) = delegate.v(fixedTag, message, throwable)
}