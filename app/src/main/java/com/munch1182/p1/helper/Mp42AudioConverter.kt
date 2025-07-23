package com.munch1182.p1.helper

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.annotation.WorkerThread
import kotlinx.coroutines.isActive
import java.io.File
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

object Mp42AudioConverter {

    private suspend fun convertStreaming(ous: OutputStream, any: MediaExtractor.() -> Unit): MediaFormat? {
        val extractor = MediaExtractor()
        var format: MediaFormat? = null
        try {
            any(extractor)

            val (audioFormat, audioTrackIndex) = findAudio(extractor)
            extractor.selectTrack(audioTrackIndex)

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: throw Exception("Invalid audio format")
            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            try {
                decoder.start()

                val info = MediaCodec.BufferInfo()

                var sawInputEOS = false
                var sawOutputEOS = false
                while (!sawOutputEOS && coroutineContext.isActive) {
                    if (!sawInputEOS) {
                        val inputBufferIndex = decoder.dequeueInputBuffer(1000L)
                        if (inputBufferIndex >= 0) {
                            val buffer = decoder.getInputBuffer(inputBufferIndex)
                            if (buffer != null) {
                                val sampleSize = extractor.readSampleData(buffer, 0)
                                if (sampleSize < 0) {
                                    decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    sawInputEOS = true
                                } else {
                                    val presentationTimeUs = extractor.sampleTime
                                    decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                                    extractor.advance()
                                }
                            }
                        }
                    }

                    val outputBufferIndex = decoder.dequeueOutputBuffer(info, 1000L)
                    if (outputBufferIndex >= 0) {
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEOS = true
                        }
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null) {
                            outputBuffer.position(info.offset)
                            outputBuffer.limit(info.offset + info.size)

                            val pcmChunk = ByteArray(info.size)
                            outputBuffer.get(pcmChunk)
                            ous.write(pcmChunk)

                            decoder.releaseOutputBuffer(outputBufferIndex, false)
                        }
                    }

                }


                format = audioFormat
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    decoder.stop()
                } catch (_: Exception) {
                }
                decoder.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            extractor.release()
        }
        return format
    }

    @WorkerThread
    suspend fun convertStreaming(video: File, ous: OutputStream): MediaFormat? {
        return convertStreaming(ous) { setDataSource(video.path) }
    }

    @WorkerThread
    suspend fun convertStreaming(ctx: Context, uri: Uri, ous: OutputStream): MediaFormat? {
        return convertStreaming(ous) { setDataSource(ctx, uri, null) }
    }

    private fun findAudio(extractor: MediaExtractor): Pair<MediaFormat, Int> {
        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }

        if (audioTrackIndex == -1 || audioFormat == null) {
            throw Exception("No audio track found")
        }
        return audioFormat to audioTrackIndex
    }
}