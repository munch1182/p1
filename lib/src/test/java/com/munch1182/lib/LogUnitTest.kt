package com.munch1182.lib

import com.munch1182.lib.base.Any2StrFmt
import org.junit.Assert.assertEquals
import org.junit.Test

class LogUnitTest {

    @Test
    fun testFMT() {
        assertEquals(Any2StrFmt.any2Str(null), "null")


        assertEquals(Any2StrFmt.any2Str(11.toByte()), "11(0x0B)")
        assertEquals(Any2StrFmt.any2Str(11.toShort()), "11(s)")
        assertEquals(Any2StrFmt.any2Str(11), "11")
        assertEquals(Any2StrFmt.any2Str(11F), "11.0F")
        assertEquals(Any2StrFmt.any2Str(11L), "11L")
        assertEquals(Any2StrFmt.any2Str(11.0), "11.0D")

        val a = 'a'
        assertEquals(Any2StrFmt.any2Str(a), "\'a\'(97)")

        assertEquals(Any2StrFmt.any2Str(mutableListOf(1, 2, 3, 4)), "[1, 2, 3, 4]")
        assertEquals(Any2StrFmt.any2Str(arrayListOf(1, 2, 5, 6)), "[1, 2, 5, 6]")
        assertEquals(Any2StrFmt.any2Str(arrayListOf(1f, 2f, 5f, 6f)), "[1.0F, 2.0F, 5.0F, 6.0F]")
        assertEquals(Any2StrFmt.any2Str(mapOf("p1" to 1L, "p2" to 2L, "p11" to 11L)), "{p1=1, p2=2, p11=11}")


        assertEquals(Any2StrFmt.any2Str(arrayOf('b', 'a', 'c', 'e')), "['b'(98), 'a'(97), 'c'(99), 'e'(101)]")
        assertEquals(Any2StrFmt.any2Str(byteArrayOf(1, 2, 3, 6, 9)), "[1(0x01), 2(0x02), 3(0x03), 6(0x06), 9(0x09)]")

        val curr = RuntimeException("err")
        println(Any2StrFmt.any2Str(curr))

        val stack = Thread.currentThread().stackTrace
        println(Any2StrFmt.any2Str(stack.first()))
    }
}