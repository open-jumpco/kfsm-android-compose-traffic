package com.example.kfsm.compose.trafficlight

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class TrafficLightModel(private val name: String) : ViewModel(), TrafficLight {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private var amberTimeoutValue: Long = 2000L
    override val amberTimeout: Long
        get() = amberTimeoutValue

    private val _red = MutableLiveData(true)
    val red: LiveData<Boolean> = _red

    private val _amber = MutableLiveData(false)
    val amber: LiveData<Boolean> = _amber

    private val _green = MutableLiveData(false)
    val green: LiveData<Boolean> = _green

    private var stoppedReceiver: (suspend () -> Unit)? = null
    private var stateReceiver: (suspend (TrafficLightStates) -> Unit)? = null
    fun changeAmberTimeout(value: Long) {
        logger.info { "changeAmberTimeout:$name:$value" }
        amberTimeoutValue = value
    }

    override suspend fun stopped() {
        logger.info { "stopped:$name" }
        stoppedReceiver?.invoke()
    }

    override suspend fun switchRed(on: Boolean) {
        logger.info { "switchRed:$name:$on" }
        viewModelScope.launch {
            _red.value = on
        }
    }

    override suspend fun switchAmber(on: Boolean) {
        logger.info { "switchAmber:$name:$on" }
        viewModelScope.launch {
            _amber.value = on
        }
    }

    override suspend fun switchGreen(on: Boolean) {
        logger.info { "switchGreen:$name:$on" }
        viewModelScope.launch {
            _green.value = on
        }
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