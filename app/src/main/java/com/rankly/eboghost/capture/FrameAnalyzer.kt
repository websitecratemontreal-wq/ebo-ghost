package com.rankly.eboghost.capture

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.media.Image
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class FrameResult(
    val bitmap: Bitmap,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class FrameAnalyzer @Inject constructor() {

    private val _latestFrame = MutableStateFlow<FrameResult?>(null)
    val latestFrame: StateFlow<FrameResult?> = _latestFrame.asStateFlow()

    /** Crop region set by calibration. Null = full frame. */
    var cropRegion: Rect? = null

    fun analyze(image: Image) {
        try {
            val bitmap = imageToBitmap(image)
            val cropped = cropRegion?.let { cropBitmap(bitmap, it) } ?: bitmap
            _latestFrame.tryEmit(FrameResult(cropped))
        } catch (e: Exception) {
            Log.e("FrameAnalyzer", "Frame analysis error", e)
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes  = image.planes
        val buffer  = planes[0].buffer
        val pixelStride   = planes[0].pixelStride
        val rowStride     = planes[0].rowStride
        val rowPadding    = rowStride - pixelStride * image.width
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun cropBitmap(src: Bitmap, region: Rect): Bitmap {
        val left   = region.left.coerceIn(0, src.width)
        val top    = region.top.coerceIn(0, src.height)
        val width  = (region.width()).coerceAtMost(src.width - left)
        val height = (region.height()).coerceAtMost(src.height - top)
        return Bitmap.createBitmap(src, left, top, width, height)
    }

    /** Returns age of last frame in ms; -1 if no frame yet. */
    fun frameAgeMs(): Long {
        val ts = _latestFrame.value?.timestamp ?: return -1L
        return System.currentTimeMillis() - ts
    }
}
