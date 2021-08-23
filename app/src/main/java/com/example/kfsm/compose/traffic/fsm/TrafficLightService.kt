package com.example.kfsm.compose.traffic.fsm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicLong

class TrafficLightService(lightName: String) : TrafficLightEventHandler {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private var _amber = MutableStateFlow(false)
    private var _red = MutableStateFlow(false)
    private var _green = MutableStateFlow(false)
    private val _state = MutableStateFlow(TrafficLightStates.OFF)
    private val _counter = AtomicLong(1)
    private val _stopped = MutableSharedFlow<Long>()
    private var amberTimeoutValue: Long = 2000L

    override val name: String = lightName
    override val amberTimeout: Long get() = amberTimeoutValue
    override val amber: StateFlow<Boolean> get() = _amber
    override val red: StateFlow<Boolean> get() = _red
    override val green: StateFlow<Boolean> get() = _green
    override val stopped: SharedFlow<Long> get() = _stopped
    override val state: StateFlow<TrafficLightStates> get() = _state

    override fun changeAmberTimeout(value: Long) {
        logger.info { "changeAmberTimeout:$name:$value" }
        amberTimeoutValue = value
    }

    override suspend fun setStopped() {
        logger.info { "stopped:$name:start" }
        CoroutineScope(Dispatchers.Default).async {
            withContext(Dispatchers.Main) {
                _stopped.emit(_counter.incrementAndGet())
            }
        }
        logger.info { "stopped:$name:done" }
    }

    override suspend fun switchRed(on: Boolean) {
        logger.info { "switchRed:$name:$on:start" }
        CoroutineScope(Dispatchers.Default).async {
            withContext(Dispatchers.Main) {
                _red.emit(on)
            }
        }
        logger.info { "switchRed:$name:$on:done" }
    }

    override suspend fun switchAmber(on: Boolean) {
        logger.info { "switchAmber:$name:$on:start" }
        CoroutineScope(Dispatchers.Default).async {
            withContext(Dispatchers.Main) {
                _amber.emit(on)
            }
        }
        logger.info { "switchAmber:$name:$on:done" }
    }

    override suspend fun switchGreen(on: Boolean) {
        logger.info { "switchGreen:$name:$on:start" }
        CoroutineScope(Dispatchers.Default).async {
            withContext(Dispatchers.Main) {
                _green.emit(on)
            }
        }
        logger.info { "switchGreen:$name:$on:end" }
    }

    override suspend fun stateChanged(toState: TrafficLightStates) {
        logger.info { "stateChanged:$name:$toState:start" }
        CoroutineScope(Dispatchers.Default).async {
            withContext(Dispatchers.Main) {
                _state.emit(toState)
            }
        }
        logger.info { "stateChanged:$name:$toState:end" }
    }
}