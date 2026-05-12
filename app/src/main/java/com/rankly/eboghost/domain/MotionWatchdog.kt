package com.rankly.eboghost.domain

import kotlinx.coroutines.*

class MotionWatchdog {

    private val timeoutMs = 3000L
    private var gestureJob: Job? = null
    private var blocked = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun canSendGesture(): Boolean = !blocked

    fun onGestureDispatched(holdMs: Long) {
        gestureJob?.cancel()
        blocked = false
        gestureJob = scope.launch {
            delay(holdMs + timeoutMs)
            blocked = true
        }
    }

    fun onGestureCompleted() {
        gestureJob?.cancel()
        blocked = false
    }

    fun reset() {
        gestureJob?.cancel()
        blocked = false
    }

    fun destroy() {
        scope.cancel()
    }
}
