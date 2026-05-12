package com.rankly.eboghost.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandBus @Inject constructor() {

    private val _commands = MutableSharedFlow<GhostCommand>(extraBufferCapacity = 32)
    val commands: SharedFlow<GhostCommand> = _commands.asSharedFlow()

    /** Enqueue a command onto the bus. Call from any coroutine context. */
    suspend fun emit(command: GhostCommand) {
        _commands.emit(command)
    }

    /** Non-suspending tryEmit — drops if buffer is full. */
    fun tryEmit(command: GhostCommand): Boolean = _commands.tryEmit(command)
}
