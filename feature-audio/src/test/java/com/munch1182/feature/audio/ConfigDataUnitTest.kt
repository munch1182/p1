package com.munch1182.feature.audio

import com.munch1182.feature.audio.convert.ConvertFormat
import com.munch1182.feature.audio.convert.Option
import com.munch1182.feature.audio.convert.ToolConfig
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

    private fun createCfg() = ToolConfig(
        cmd = "ffmpeg -y -i {{from}} {{format}} {{to}}",
        params = buildMap {
            put("encoding", listOf(Option("16 kHz", "16000")))
            put("channel", listOf(Option("MONO", "0"), Option("STEREO", "1")))
        },
        formats = listOf(
            ConvertFormat("1", "pcm", listOf("encoding", "channel"), "-e pcm -c {{channel}}"),
            ConvertFormat("2", "mp3", listOf("encoding", "channel"), "-e mp3 -c {{channel}}"),
            ConvertFormat("3", "wav", listOf("encoding", "channel"), "-e wav"),
        )
    )
}