package com.munch1182.lib

import com.munch1182.lib.helper.FileHelper
import org.junit.Test
import java.io.File

class FileUnitTest {

    @Test
    fun testUnzip() {
        val file = File(".\\src\\test\\java\\com\\munch1182\\lib\\test.zip")
        println(file.absolutePath)
        val to = File(".\\src\\test\\java\\com\\munch1182\\lib\\test_unzip")
        assert(FileHelper.unzip(file, to) != null)
    }
}