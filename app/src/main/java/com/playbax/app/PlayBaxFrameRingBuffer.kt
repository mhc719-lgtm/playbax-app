package com.playbax.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.abs

class FramePacket(
    val id: Long,
    val timestampMs: Long,
    val jpegBytes: ByteArray
)

class DelayedFrameResult(
    val packetId: Long,
    val bitmap: Bitmap?,
    val isWarmingUp: Boolean,
    val remainingWarmupSeconds: Float,
    val bufferFillProgress: Float
)

/**
 * Thread-safe, memory-capped circular ring buffer holding HD compressed JPEG frames in RAM
 * for 1-30s (up to 3x cascading screens = 90s). Decodes full 32-bit ARGB_8888 bitmaps
 * on a background worker thread so the UI thread never blocks.
 */
class PlayBaxFrameRingBuffer {

    companion object {
        private const val MIN_FRAME_INTERVAL_MS = 40L // ~25 fps smooth HD target
    }

    private val maxMemoryBytes: Long = run {
        val maxHeap = Runtime.getRuntime().maxMemory()
        (maxHeap / 2L).coerceIn(96L * 1024L * 1024L, 192L * 1024L * 1024L)
    }

    private val lock = Any()
    private val frames = ArrayList<FramePacket>(1200)
    private var totalBytesUsed: Long = 0L
    private var lastPushedTimestampMs: Long = 0L
    private var nextPacketId: Long = 1L

    fun shouldAcceptFrame(nowMs: Long): Boolean {
        synchronized(lock) {
            return (nowMs - lastPushedTimestampMs) >= MIN_FRAME_INTERVAL_MS
        }
    }

    fun pushFrame(jpegBytes: ByteArray, timestampMs: Long, maxDelayMs: Long) {
        synchronized(lock) {
            lastPushedTimestampMs = timestampMs
            val pkt = FramePacket(nextPacketId++, timestampMs, jpegBytes)
            frames.add(pkt)
            totalBytesUsed += jpegBytes.size

            val cutoffMs = timestampMs - (maxDelayMs + 1500L)
            var removeCount = 0
            while (removeCount < frames.size - 2) {
                val old = frames[removeCount]
                if (old.timestampMs < cutoffMs || totalBytesUsed > maxMemoryBytes) {
                    totalBytesUsed -= old.jpegBytes.size
                    removeCount++
                } else {
                    break
                }
            }
            if (removeCount > 0) {
                frames.subList(0, removeCount).clear()
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            frames.clear()
            totalBytesUsed = 0L
            lastPushedTimestampMs = 0L
        }
    }

    fun decodeFrameForDelay(
        nowMs: Long,
        delayMs: Long,
        lastPacketId: Long,
        currentBitmap: Bitmap?
    ): DelayedFrameResult {
        val packet: FramePacket
        val warmingUp: Boolean
        val remainingSec: Float
        val fillProgress: Float

        synchronized(lock) {
            if (frames.isEmpty()) {
                return DelayedFrameResult(-1L, null, true, delayMs / 1000f, 0f)
            }
            val oldestMs = frames.first().timestampMs
            val spanMs = (nowMs - oldestMs).coerceAtLeast(0L)
            if (spanMs < delayMs) {
                packet = frames.last()
                warmingUp = true
                remainingSec = ((delayMs - spanMs).coerceAtLeast(0L)) / 1000f
                fillProgress = (spanMs.toFloat() / delayMs.toFloat()).coerceIn(0f, 1f)
            } else {
                val targetMs = nowMs - delayMs
                packet = findClosestPacketLocked(targetMs)
                warmingUp = false
                remainingSec = 0f
                fillProgress = 1f
            }
        }

        if (packet.id == lastPacketId && currentBitmap != null && !currentBitmap.isRecycled) {
            return DelayedFrameResult(packet.id, currentBitmap, warmingUp, remainingSec, fillProgress)
        }

        val decoded = decodeJpegNewBitmap(packet.jpegBytes)
        return DelayedFrameResult(
            packetId = packet.id,
            bitmap = decoded ?: currentBitmap,
            isWarmingUp = warmingUp,
            remainingWarmupSeconds = remainingSec,
            bufferFillProgress = fillProgress
        )
    }

    private fun findClosestPacketLocked(targetMs: Long): FramePacket {
        var low = 0
        var high = frames.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val midTime = frames[mid].timestampMs
            when {
                midTime < targetMs -> low = mid + 1
                midTime > targetMs -> high = mid - 1
                else -> return frames[mid]
            }
        }
        val idx1 = low.coerceIn(0, frames.size - 1)
        val idx2 = high.coerceIn(0, frames.size - 1)
        return if (abs(frames[idx1].timestampMs - targetMs) <= abs(frames[idx2].timestampMs - targetMs)) {
            frames[idx1]
        } else {
            frames[idx2]
        }
    }

    private fun decodeJpegNewBitmap(bytes: ByteArray): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Exception) {
            null
        }
    }
}
