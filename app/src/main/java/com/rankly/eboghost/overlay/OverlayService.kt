package com.rankly.eboghost.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.rankly.eboghost.R
import com.rankly.eboghost.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var overlayController: OverlayController

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    companion object {
        const val CHANNEL_ID = "ebo_ghost_overlay"
        const val NOTIF_ID  = 1001
        const val ACTION_STOP = "com.rankly.eboghost.STOP_OVERLAY"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
        attachOverlay()
    }

    private fun startForegroundWithNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID, "EBO Ghost Overlay",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EBO Ghost Active")
            .setContentText("Overlay running. Tap to open.")
            .setSmallIcon(R.drawable.ic_ghost_notification)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    private fun attachOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0; y = 100
        }

        // Inflate the overlay Compose host view
        val view = LayoutInflater.from(this).inflate(
            R.layout.overlay_container, null, false
        )
        setupDrag(view, params)
        windowManager.addView(view, params)
        overlayView = view
        overlayController.attachView(view)
    }

    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { windowManager.removeView(it) }
        overlayController.detach()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
