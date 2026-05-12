package com.rankly.eboghost.control

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.rankly.eboghost.domain.CalibrationStore
import com.rankly.eboghost.domain.CommandBus
import com.rankly.eboghost.domain.GhostCommand
import com.rankly.eboghost.domain.MotionWatchdog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class ControlAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ControlAccessibilityService? = null
        private const val TAG = "ControlAccessSvc"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var commandBus: CommandBus
    private lateinit var calibrationStore: CalibrationStore
    private lateinit var motionWatchdog: MotionWatchdog

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        commandBus = CommandBus.getInstance()
        calibrationStore = CalibrationStore.getInstance(applicationContext)
        motionWatchdog = MotionWatchdog()
        serviceScope.launch {
            commandBus.commands.collectLatest { cmd ->
                handleCommand(cmd)
            }
        }
    }

    private fun handleCommand(cmd: GhostCommand) {
        when (cmd) {
            is GhostCommand.Move -> dispatchGestureCommand(cmd.direction, cmd.holdMs)
            is GhostCommand.Stop -> motionWatchdog.reset()
            is GhostCommand.EmergencyStop -> motionWatchdog.reset()
            else -> Unit
        }
    }

    private fun dispatchGestureCommand(key: String, holdMs: Long) {
        if (!motionWatchdog.canSendGesture()) {
            android.util.Log.w(TAG, "Watchdog blocked gesture: $key")
            return
        }
        val point = calibrationStore.getPoint(key)
        if (point == null) {
            android.util.Log.w(TAG, "No calibration for $key")
            return
        }
        val path = Path().apply { moveTo(point.first.toFloat(), point.second.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, holdMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                motionWatchdog.onGestureCompleted()
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                android.util.Log.w(TAG, "Gesture cancelled: $key")
            }
        }, null)
        motionWatchdog.onGestureDispatched(holdMs)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
