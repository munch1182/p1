package com.munch1182.p1.measure

import com.munch1182.lib.base.ILog
import java.util.concurrent.atomic.AtomicInteger

object MeasureHelper {

    private var log: ILog? = null
    private var start = 0L
    private var timePoint = HashMap<Int, Long>()
    private var id = AtomicInteger(0)

    fun setLog(log: ILog) {
        this.log = log
    }

    fun measureStart() {
        timePoint[id.incrementAndGet()] = System.currentTimeMillis() - start
        log?.logStr("Measure Start")
    }

    fun measureNow(point: String? = null) {
        timePoint[id.incrementAndGet()] = System.currentTimeMillis()
        log?.logStr("Measured ${time()} ms ${point?.let { "when $it" } ?: ""}")
    }

    fun measureEnd() {
        if (timePoint.size > 1) {
            val start = timePoint[1]!!
            if (start > 0L) {
                log?.logStr("Measured ${total()} ms, end.")
            }
            timePoint.clear()
            id.set(0)
        }
    }

    private fun total(): Long {
        if (timePoint.size <= 1) return 0
        val i = id.get()
        return if (i == 0) 0 else (timePoint[i]!! - timePoint[1]!!)
    }

    private fun time(): Long {
        if (timePoint.size <= 1) return 0
        val i = id.get()
        return if (i == 0) 0 else (timePoint[i]!! - timePoint[i - 1]!!)
    }
}