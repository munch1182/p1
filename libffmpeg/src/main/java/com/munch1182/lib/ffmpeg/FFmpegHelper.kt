package com.munch1182.lib.ffmpeg

import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFprobe

object FFmpegHelper {

    fun executeCmd(cmd: String) {
        FFmpeg.execute(cmd.removePrefix("ffmpeg "))
    }

    fun executeFFprobeCmd(cmd: String) {
        FFprobe.execute(cmd.removePrefix("ffprobe "))
    }

    fun getMediaInfo(path: String): String? {
        val cmd = "ffprobe -v error -show_format -show_streams $path -print_format json"
        return FFprobe.getMediaInformationFromCommand(cmd.removePrefix("ffprobe "))?.allProperties?.toString()
    }
}