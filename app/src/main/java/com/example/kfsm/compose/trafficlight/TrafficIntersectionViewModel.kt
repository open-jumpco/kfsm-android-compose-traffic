package com.example.kfsm.compose.trafficlight

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mu.KotlinLogging


class TrafficIntersectionViewModel constructor(
    private val trafficIntersectionModel: TrafficIntersectionModel
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val _intersectionState = MutableLiveData(trafficIntersectionModel.currentState)
    private val _allowStart = MutableLiveData(false)
    private val _allowSwitch = MutableLiveData(false)
    private val _allowStop = MutableLiveData(false)
    private var _trafficLightData: List<MutableLiveData<TrafficLightViewModel>>
    val intersectionState: LiveData<IntersectionStates> get() = _intersectionState
    val allowStart: LiveData<Boolean> get() = _allowStart
    val allowSwitch: LiveData<Boolean> get() = _allowSwitch
    val allowStop: LiveData<Boolean> get() = _allowStop
    val trafficLightData: List<LiveData<TrafficLightViewModel>> get() = _trafficLightData

    val amberTimeout: Long get() = trafficIntersectionModel.amberTimeout
    val cycleWaitTime: Long get() = trafficIntersectionModel.cycleWaitTime
    val cycleTime: Long get() = trafficIntersectionModel.cycleTime
    val currentName: String get() = trafficIntersectionModel.currentName

    val trafficLights: List<TrafficLightViewModel> = trafficIntersectionModel.listOrder.map {
        TrafficLightViewModel(
            trafficIntersectionModel.get(it)
        )
    }.toList()

    init {
        _trafficLightData = trafficLights.map { MutableLiveData(it) }.toList()
        trafficLights.forEach {
            val lightViewModel = it
            it.setNotifyStateChange {
                viewModelScope.launch {
                    lightViewModel.updateState()
                }
            }
        }
        trafficIntersectionModel.setNotifyStopped {
            logger.info("stopped")
            viewModelScope.launch {
                trafficIntersectionModel.stopped()
            }
        }
        trafficIntersectionModel.setNotifyStateChange { toState ->
            logger.info { "stateChanged:$toState" }
            viewModelScope.launch {
                determineAllowed()
                _intersectionState.value = toState
            }
        }
        viewModelScope.launch {
            determineAllowed()
        }
    }

    private fun determineAllowed() {
        val allowedEvents = trafficIntersectionModel.allowedEvents()
        _allowStart.value = allowedEvents.contains(IntersectionEvents.START)
        _allowStop.value = allowedEvents.contains(IntersectionEvents.STOP)
        _allowSwitch.value = allowedEvents.contains(IntersectionEvents.SWITCH)
        logger.info { "determineAllowed:start:${_allowStart.value}" }
        logger.info { "determineAllowed:stop:${_allowStop.value}" }
        logger.info { "determineAllowed:switch:${_allowSwitch.value}" }
    }

    suspend fun startSystem() {
        trafficIntersectionModel.startSystem()
    }

    suspend fun stopSystem() {
        trafficIntersectionModel.stopSystem()
    }

    suspend fun switch() {
        trafficIntersectionModel.switch()
    }

}