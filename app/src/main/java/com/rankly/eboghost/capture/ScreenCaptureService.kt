package com.rankly.eboghost.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.rankly.eboghost.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScreenCaptureService : Service() {

    @Inject lateinit var frameAnalyzer: FrameAnalyzer

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    companion object {
        const val CHANNEL_ID   = "ebo_ghost_capture"
        const val NOTIF_ID     = 1002
        const val EXTRA_RESULT = "result_code"
        const val EXTRA_DATA   = "result_data"
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT, -1) ?: return START_NOT_STICKY
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_DATA) ?: return START_NOT_STICKY

        val projManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = projManager.getMediaProjection(resultCode, resultData)
        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WindowManager::class.java)).defaultDisplay.getMetrics(metrics)

        val width  = metrics.widthPixels
        val height = metrics.heightPixels
        val dpi    = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader!!.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            frameAnalyzer.analyze(image)
            image.close()
        }, null)

        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "EboGhostCapture",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null, null
        )
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "EBO Ghost Capture", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EBO Ghost Capture")
            .setContentText("Screen capture active.")
            .setSmallIcon(R.drawable.ic_ghost_notification)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
