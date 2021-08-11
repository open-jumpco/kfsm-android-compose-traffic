package com.example.kfsm.compose.trafficlight

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class TrafficLightViewModel(private val trafficLight: TrafficLight) {
    private val _red = MutableLiveData(true)
    private val _green = MutableLiveData(false)
    private val _amber = MutableLiveData(false)

    val name: String = trafficLight.name
    val red: LiveData<Boolean> = _red
    val amber: LiveData<Boolean> = _amber
    val green: LiveData<Boolean> = _green

    fun setNotifyStateChange(receiver: suspend (newState: TrafficLightStates) -> Unit) =
        trafficLight.setNotifyStateChange(receiver)

    fun updateState() {
        _red.value = trafficLight.red
        _amber.value = trafficLight.amber
        _green.value = trafficLight.green
    }
}