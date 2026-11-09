package com.omnitools.app.tools.media

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** What a video file contains, as far as sound goes. */
class Probe(val durationUs: Long, val audioMime: String?, val sampleRate: Int, val channels: Int, val hasVideo: Boolean)

sealed class ExtractResult {
    class Done(val file: File, val container: Container, val note: String?) : ExtractResult()
    class Failed(val message: String) : ExtractResult()
    object Cancelled : ExtractResult()
}

private class Cancel : RuntimeException()
private class Problem(message: String) : RuntimeException(message)

/** Pulls the sound out of a video on this phone using Android's own media code. Nothing leaves the device. */
object AudioExtractor {
    private fun open(context: Context, uri: Uri): Pair<MediaExtractor, android.os.ParcelFileDescriptor> {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: throw Problem("The file could not be opened.")
        val ex = MediaExtractor()
        try { ex.setDataSource(pfd.fileDescriptor) } catch (e: Exception) { ex.release(); pfd.close(); throw Problem("This file could not be read as a video.") }
        return ex to pfd
    }

    private fun audioTrack(ex: MediaExtractor): Int =
        (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true } ?: -1

    fun probe(context: Context, uri: Uri): Probe {
        val (ex, pfd) = open(context, uri)
        try {
            val a = audioTrack(ex)
            val hasVideo = (0 until ex.trackCount).any { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
            if (a < 0) return Probe(0, null, 0, 0, hasVideo)
            val f = ex.getTrackFormat(a)
            return Probe(
                durationUs = if (f.containsKey(MediaFormat.KEY_DURATION)) f.getLong(MediaFormat.KEY_DURATION) else 0,
                audioMime = f.getString(MediaFormat.KEY_MIME),
                sampleRate = if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) f.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 0,
                channels = if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) f.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 0,
                hasVideo = hasVideo,
            )
        } finally { ex.release(); pfd.close() }
    }

    /** Extracts the sound to [out]'s folder. [out] has no extension yet; the final file name is chosen by [nameFor]. */
    fun extract(
        context: Context, uri: Uri, dir: File, nameFor: (Container) -> String, wantWav: Boolean,
        onProgress: (Float) -> Unit, isCancelled: () -> Boolean,
    ): ExtractResult {
        dir.mkdirs()
        var temp: File? = null
        try {
            val (ex, pfd) = open(context, uri)
            try {
                val track = audioTrack(ex)
                if (track < 0) return ExtractResult.Failed("This video has no sound track.")
                val format = ex.getTrackFormat(track)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                val duration = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
                ex.selectTrack(track)

                var container = AudioPlan.choose(mime, Build.VERSION.SDK_INT, wantWav)
                var note: String? = null
                if (container != Container.WAV) {
                    temp = File(dir, ".working-${System.nanoTime()}")
                    try {
                        copyTrack(ex, format, container, temp, duration, onProgress, isCancelled)
                    } catch (c: Cancel) { throw c } catch (e: Exception) {
                        // The container refused this stream: fall back to a plain WAV, which always works.
                        temp.delete()
                        container = Container.WAV
                        note = "The sound could not be copied as is, so it was converted to WAV."
                        ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    }
                }
                if (container == Container.WAV) {
                    temp = File(dir, ".working-${System.nanoTime()}")
                    decodeToWav(ex, format, mime, temp, duration, onProgress, isCancelled)
                }
                val finalFile = File(dir, nameFor(container))
                if (!temp!!.renameTo(finalFile)) { temp.copyTo(finalFile, overwrite = true); temp.delete() }
                temp = null
                onProgress(1f)
                return ExtractResult.Done(finalFile, container, note)
            } finally { ex.release(); pfd.close() }
        } catch (c: Cancel) {
            return ExtractResult.Cancelled
        } catch (p: Problem) {
            return ExtractResult.Failed(p.message ?: "Extraction failed.")
        } catch (e: OutOfMemoryError) {
            return ExtractResult.Failed("Not enough memory to process this file.")
        } catch (e: Exception) {
            return ExtractResult.Failed("Extraction failed: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            try { temp?.delete() } catch (_: Exception) {}
        }
    }

    private fun copyTrack(
        ex: MediaExtractor, format: MediaFormat, container: Container, out: File, durationUs: Long,
        onProgress: (Float) -> Unit, isCancelled: () -> Boolean,
    ) {
        val fmt = when (container) {
            Container.M4A -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            Container.OGG -> MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG
            else -> MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        }
        val muxer = MediaMuxer(out.absolutePath, fmt)
        var started = false
        var finished = false
        var written = 0
        try {
            val idx = muxer.addTrack(format)
            muxer.start(); started = true
            val size = maxOf(1 shl 20, if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0)
            val buf = ByteBuffer.allocate(size)
            val info = MediaCodec.BufferInfo()
            var count = 0
            while (true) {
                if (isCancelled()) throw Cancel()
                buf.clear()
                val n = ex.readSampleData(buf, 0)
                if (n < 0) break
                val t = ex.sampleTime
                if (t >= 0) {
                    info.set(0, n, t, if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                    muxer.writeSampleData(idx, buf, info)
                    written++
                    if (durationUs > 0 && count++ % 20 == 0) onProgress((t.toFloat() / durationUs).coerceIn(0f, 0.99f))
                }
                ex.advance()
            }
            if (written == 0) throw Problem("No sound data found.")
            muxer.stop()          // a failure here must surface, so the caller falls back to WAV instead of keeping a broken file
            finished = true
        } finally {
            if (started && !finished) try { muxer.stop() } catch (_: Exception) {}
            try { muxer.release() } catch (_: Exception) {}
        }
    }

    private fun decodeToWav(
        ex: MediaExtractor, format: MediaFormat, mime: String, out: File, durationUs: Long,
        onProgress: (Float) -> Unit, isCancelled: () -> Boolean,
    ) {
        val codec = try { MediaCodec.createDecoderByType(mime) } catch (e: Exception) { throw Problem("This phone cannot decode the sound in this video (${AudioPlan.codecName(mime)}).") }
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            var rate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            var float = false
            val info = MediaCodec.BufferInfo()
            var inputDone = false; var outputDone = false; var written = 0L; var idle = 0
            var scratch = ByteArray(1 shl 16)
            RandomAccessFile(out, "rw").use { raf ->
                raf.setLength(0)
                raf.write(ByteArray(44))
                while (!outputDone) {
                    if (isCancelled()) throw Cancel()
                    if (!inputDone) {
                        val i = codec.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val ib = codec.getInputBuffer(i)!!
                            val n = ex.readSampleData(ib, 0)
                            if (n < 0) { codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                            else {
                                val t = ex.sampleTime
                                codec.queueInputBuffer(i, 0, n, maxOf(t, 0), 0)
                                if (durationUs > 0 && t >= 0) onProgress((t.toFloat() / durationUs).coerceIn(0f, 0.99f))
                                ex.advance()
                            }
                        }
                    }
                    val o = codec.dequeueOutputBuffer(info, 10_000)
                    when {
                        o >= 0 -> {
                            idle = 0
                            val ob = codec.getOutputBuffer(o)
                            if (ob != null && info.size > 0) {
                                ob.position(info.offset); ob.limit(info.offset + info.size); ob.order(ByteOrder.LITTLE_ENDIAN)
                                if (!float) {
                                    val n = info.size
                                    if (scratch.size < n) scratch = ByteArray(n)
                                    ob.get(scratch, 0, n)
                                    raf.write(scratch, 0, n); written += n
                                } else {
                                    val fb = ob.asFloatBuffer()
                                    val n = fb.remaining()
                                    if (scratch.size < n * 2) scratch = ByteArray(n * 2)
                                    for (k in 0 until n) {
                                        val v = (fb.get() * 32767f).toInt().coerceIn(-32768, 32767)
                                        scratch[k * 2] = v.toByte(); scratch[k * 2 + 1] = (v shr 8).toByte()
                                    }
                                    raf.write(scratch, 0, n * 2); written += n * 2
                                }
                                if (written > 2_000_000_000L) throw Problem("The audio would be larger than 2 GB. Use the original-quality option instead.")
                            }
                            codec.releaseOutputBuffer(o, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        }
                        o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val f = codec.outputFormat
                            if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            float = f.containsKey(MediaFormat.KEY_PCM_ENCODING) && f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                        }
                        else -> if (inputDone && ++idle > 500) throw Problem("The sound could not be decoded.")
                    }
                }
                if (written == 0L) throw Problem("No sound could be decoded from this video.")
                raf.seek(0)
                raf.write(AudioPlan.wavHeader(rate, channels, written))
            }
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
        }
    }
}
