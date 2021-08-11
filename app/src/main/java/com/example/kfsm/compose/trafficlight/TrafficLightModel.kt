package com.example.kfsm.compose.trafficlight

import mu.KotlinLogging

class TrafficLightModel(lightName: String) : TrafficLight {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    private var _amber: Boolean = false
    private var _red: Boolean = false
    private var _green: Boolean = false
    override val amber: Boolean
        get() = _amber

    override val red: Boolean
        get() = _red

    override val green: Boolean
        get() = _green

    override val name: String = lightName
    private var amberTimeoutValue: Long = 2000L
    override val amberTimeout: Long
        get() = amberTimeoutValue

    private var stoppedReceiver: (suspend () -> Unit)? = null
    private var stateReceiver: (suspend (TrafficLightStates) -> Unit)? = null
    override fun changeAmberTimeout(value: Long) {
        logger.info { "changeAmberTimeout:$name:$value" }
        amberTimeoutValue = value
    }

    override suspend fun stopped() {
        logger.info { "stopped:$name" }
        stoppedReceiver?.invoke()
    }

    override suspend fun switchRed(on: Boolean) {
        logger.info { "switchRed:$name:$on" }
        _red = on
    }

    override suspend fun switchAmber(on: Boolean) {
        logger.info { "switchAmber:$name:$on" }
        _amber = on
    }

    override suspend fun switchGreen(on: Boolean) {
        logger.info { "switchGreen:$name:$on" }
        _green = on
    }

    override fun setNotifyStopped(receiver: suspend () -> Unit) {
        stoppedReceiver = receiver
    }

    override fun setNotifyStateChange(receiver: suspend (newState: TrafficLightStates) -> Unit) {
        stateReceiver = receiver
    }

    override suspend fun stateChanged(toState: TrafficLightStates) {
        logger.info { "stateChanged:$name:$toState" }
        stateReceiver?.invoke(toState)
    }
}