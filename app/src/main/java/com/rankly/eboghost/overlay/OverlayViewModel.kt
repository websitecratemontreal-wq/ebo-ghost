package com.rankly.eboghost.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rankly.eboghost.domain.CommandBus
import com.rankly.eboghost.domain.GhostCommand
import com.rankly.eboghost.domain.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OverlayUiState(
    val isCapturing: Boolean = false,
    val isCalibrating: Boolean = false,
    val statusMessage: String = "Ready"
)

class OverlayViewModel(
    private val commandBus: CommandBus,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState

    fun sendMove(direction: String, holdMs: Long = 500L) {
        viewModelScope.launch {
            commandBus.send(GhostCommand.Move(direction, holdMs))
        }
    }

    fun emergencyStop() {
        viewModelScope.launch {
            commandBus.send(GhostCommand.EmergencyStop)
            ttsManager.speak("Emergency stop")
        }
    }

    fun speak(text: String) {
        ttsManager.speak(text)
    }

    fun toggleCalibration() {
        _uiState.value = _uiState.value.copy(
            isCalibrating = !_uiState.value.isCalibrating
        )
    }

    fun setCapturing(active: Boolean) {
        _uiState.value = _uiState.value.copy(isCapturing = active)
    }

    fun setStatus(msg: String) {
        _uiState.value = _uiState.value.copy(statusMessage = msg)
    }
}
