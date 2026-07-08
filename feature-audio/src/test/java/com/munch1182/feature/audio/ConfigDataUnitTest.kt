package com.munch1182.feature.audio

import com.munch1182.feature.audio.convert.ConvertConfig
import com.munch1182.feature.audio.convert.ConvertFormat
import kotlinx.serialization.json.Json
import org.junit.Test

class ConfigDataUnitTest {

    @Test
    fun parse_isCorrect() {
    }

    @Test
    fun json_isCorrect() {
        val cfg = createCfg()
        println(Json.encodeToString(cfg))
    }

    private fun createCfg() = ConvertConfig(
        params = mapOf(
            "encoding" to listOf("16000", "32000", "64000"),
            "channel" to listOf("MONO", "STEREO")
        ),
        formats = listOf(
            ConvertFormat(id = "1", name = "pcm", params = listOf("encoding", "channel"), cmd = "-e pcm -c {{channel}}"),
            ConvertFormat(id = "2", name = "mp3", params = listOf("encoding", "channel"), cmd = "-e mp3 -c {{channel}}"),
            ConvertFormat(id = "3", name = "wav", params = listOf("encoding", "channel"), cmd = "-e wav"),
        )
    )
}