package com.playbax.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Surface
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Hardware H.264 Surface encoder + MediaMuxer that records the composite 1, 2, or 3
 * delayed playback screens (Stacked or Side-by-Side) directly into Movies/PlayBax.
 */
class PlayBaxStageRecorder(
    private val context: Context,
    private val onStatusToast: (String) -> Unit,
    private val onRecordingStateChanged: (Boolean) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val dividerPaint = Paint().apply { color = Color.parseColor("#26FFFFFF") }
    private val srcRect = Rect()
    private val dstRect = RectF()

    @Volatile var isRecording: Boolean = false
        private set

    private var encoder: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var muxer: MediaMuxer? = null
    private var muxerStarted: Boolean = false
    private var videoTrackIndex: Int = -1
    private var tempFile: File? = null
    private var videoWidth: Int = 720
    private var videoHeight: Int = 1280
    private val bufferInfo = MediaCodec.BufferInfo()

    fun startRecording(isLandscape: Boolean) {
        synchronized(lock) {
            if (isRecording) return
            try {
                videoWidth = if (isLandscape) 1280 else 720
                videoHeight = if (isLandscape) 720 else 1280

                val outTemp = File(context.cacheDir, "PlayBax_rec_${System.currentTimeMillis()}.mp4")
                tempFile = outTemp

                val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, 24)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }

                val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val surface = codec.createInputSurface()
                codec.start()

                val mediaMuxer = MediaMuxer(outTemp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                encoder = codec
                inputSurface = surface
                muxer = mediaMuxer
                muxerStarted = false
                videoTrackIndex = -1
                isRecording = true

                mainHandler.post { onRecordingStateChanged(true) }
            } catch (_: Exception) {
                releaseLocked()
                mainHandler.post { onRecordingStateChanged(false) }
            }
        }
    }

    fun recordStageFrame(
        slotBitmaps: Array<Bitmap?>,
        screenCount: Int,
        sideBySide: Boolean
    ) {
        synchronized(lock) {
            if (!isRecording) return
            val surface = inputSurface ?: return
            try {
                val canvas = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        surface.lockHardwareCanvas()
                    } else {
                        surface.lockCanvas(null)
                    }
                } catch (_: Exception) {
                    surface.lockCanvas(null)
                } ?: return

                try {
                    val w = videoWidth.toFloat()
                    val h = videoHeight.toFloat()
                    canvas.drawColor(Color.BLACK)

                    val clampedCount = screenCount.coerceIn(1, 3)
                    for (i in 0 until clampedCount) {
                        val pLeft = if (sideBySide) (w * i) / clampedCount else 0f
                        val pTop = if (sideBySide) 0f else (h * i) / clampedCount
                        val pRight = if (sideBySide) (w * (i + 1)) / clampedCount else w
                        val pBottom = if (sideBySide) h else (h * (i + 1)) / clampedCount
                        val pW = pRight - pLeft
                        val pH = pBottom - pTop

                        val bmp = slotBitmaps.getOrNull(i)
                        if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
                            canvas.save()
                            canvas.clipRect(pLeft, pTop, pRight, pBottom)
                            srcRect.set(0, 0, bmp.width, bmp.height)
                            val scale = max(pW / bmp.width.toFloat(), pH / bmp.height.toFloat())
                            val scaledW = bmp.width * scale
                            val scaledH = bmp.height * scale
                            val dLeft = pLeft + (pW - scaledW) * 0.5f
                            val dTop = pTop + (pH - scaledH) * 0.5f
                            dstRect.set(dLeft, dTop, dLeft + scaledW, dTop + scaledH)
                            canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
                            canvas.restore()
                        }

                        if (i < clampedCount - 1) {
                            if (sideBySide) {
                                canvas.drawRect(pRight - 1f, 0f, pRight + 1f, h, dividerPaint)
                            } else {
                                canvas.drawRect(0f, pBottom - 1f, w, pBottom + 1f, dividerPaint)
                            }
                        }
                    }
                } finally {
                    surface.unlockCanvasAndPost(canvas)
                }

                drainEncoderLocked(endOfStream = false)
            } catch (_: Exception) {
            }
        }
    }

    fun stopRecording(silent: Boolean = false) {
        val fileToPublish: File?
        val hadFrames: Boolean
        synchronized(lock) {
            if (!isRecording) return
            try {
                drainEncoderLocked(endOfStream = true)
            } catch (_: Exception) {
            }
            fileToPublish = tempFile
            hadFrames = muxerStarted
            releaseLocked()
        }

        var publishedOk = false
        if (hadFrames && fileToPublish != null && fileToPublish.exists() && fileToPublish.length() > 0L) {
            publishedOk = publishToMediaStore(fileToPublish)
        }
        try { fileToPublish?.delete() } catch (_: Exception) {}

        mainHandler.post {
            onRecordingStateChanged(false)
            if (!silent && publishedOk) {
                onStatusToast("Saved MP4 to Movies/PlayBax")
            }
        }
    }

    private fun drainEncoderLocked(endOfStream: Boolean) {
        val codec = encoder ?: return
        val mediaMuxer = muxer ?: return
        if (endOfStream) {
            try { codec.signalEndOfInputStream() } catch (_: Exception) {}
        }

        var loops = 0
        while (loops < 60) {
            loops++
            val status = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) 2500L else 0L)
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!muxerStarted) {
                    videoTrackIndex = mediaMuxer.addTrack(codec.outputFormat)
                    mediaMuxer.start()
                    muxerStarted = true
                }
            } else if (status >= 0) {
                val encodedData = codec.getOutputBuffer(status)
                if (encodedData != null) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                }
                codec.releaseOutputBuffer(status, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
            }
        }
    }

    private fun releaseLocked() {
        isRecording = false
        try { encoder?.stop() } catch (_: Exception) {}
        try { encoder?.release() } catch (_: Exception) {}
        encoder = null
        try { inputSurface?.release() } catch (_: Exception) {}
        inputSurface = null
        if (muxerStarted) {
            try { muxer?.stop() } catch (_: Exception) {}
        }
        try { muxer?.release() } catch (_: Exception) {}
        muxer = null
        muxerStarted = false
        videoTrackIndex = -1
    }

    private fun publishToMediaStore(sourceFile: File): Boolean {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "PlayBax_$timestamp.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PlayBax")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input -> input.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
