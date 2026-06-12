package com.munch1182.lib.common

import java.util.concurrent.atomic.AtomicLong

class Timer(
    private val tag: String,
    private val log: Logger,
    private val strict: Boolean = false  // true: 严格模式抛异常; false: 静默容错
) {
    private val startTime = AtomicLong(0L)

    companion object {
        private const val TAG = "Timer"
    }

    /**
     * 开始计时。
     * - 严格模式(enableDebug=true)下重复调用抛出 IllegalStateException
     * - 非严格模式下重复调用只记录警告
     */
    fun start() {
        val now = System.nanoTime()
        if (!startTime.compareAndSet(0L, now)) {
            if (strict) {
                throw IllegalStateException("Timer tag($tag) already started")
            } else {
                log.e(TAG, "Timer tag($tag) already started, ignoring duplicate start")
                return
            }
        }
        if (!strict) {
            log.d(TAG, "Timer start tag($tag)")
        }
    }

    /**
     * 停止计时，返回耗时（毫秒）。
     * - 严格模式下若未开始或已结束，抛出 IllegalStateException
     * - 非严格模式下返回 -1 并记录警告
     */
    fun end(): Long {
        val start = startTime.getAndSet(0L)
        if (start == 0L) {
            if (strict) {
                throw IllegalStateException("Timer tag($tag) not started or already ended")
            } else {
                log.e(TAG, "Timer tag($tag) end called but not started or already ended")
                return -1L
            }
        }
        val costMillis = (System.nanoTime() - start) / 1_000_000L
        if (!strict) {
            log.d(TAG, "Timer end tag($tag) cost: $costMillis ms")
        }
        startTime.set(0L)
        return costMillis
    }

    /**
     * 查询已用时间（毫秒），不停止计时。
     */
    fun elapsedMillis(): Long {
        val start = startTime.get()
        if (start == 0L) {
            if (strict) {
                throw IllegalStateException("Timer tag($tag) not started")
            } else {
                log.e(TAG, "Timer tag($tag) not started")
                return -1L
            }
        }
        return (System.nanoTime() - start) / 1_000_000L
    }
}