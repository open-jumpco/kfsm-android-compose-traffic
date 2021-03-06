package com.example.kfsm.compose.traffic

import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kfsm.compose.traffic.fsm.IntersectionEvents
import com.example.kfsm.compose.traffic.fsm.IntersectionStates
import com.example.kfsm.compose.traffic.fsm.TrafficIntersectionEventHandler
import com.example.kfsm.compose.traffic.fsm.TrafficLightEventHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import mu.KotlinLogging


class TrafficIntersectionViewModel constructor(
    private val trafficIntersection: TrafficIntersectionEventHandler
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val _allowStart = MutableStateFlow(false)
    private val _allowSwitch = MutableStateFlow(false)
    private val _allowStop = MutableStateFlow(false)
    val intersectionState: StateFlow<IntersectionStates> get() = trafficIntersection.state
    val allowStart: StateFlow<Boolean> get() = _allowStart
    val allowSwitch: StateFlow<Boolean> get() = _allowSwitch
    val allowStop: StateFlow<Boolean> get() = _allowStop
    val amberTimeout: Long get() = trafficIntersection.amberTimeout
    val cycleWaitTime: Long get() = trafficIntersection.cycleWaitTime
    val cycleTime: Long get() = trafficIntersection.cycleTime
    val currentName: String get() = trafficIntersection.currentName

    val trafficLights: List<TrafficLightEventHandler> = trafficIntersection.trafficLights

    private suspend fun determineAllowed() {
        val allowedEvents = trafficIntersection.allowedEvents()
        val allowedStart = allowedEvents.contains(IntersectionEvents.START)
        val allowedSwitch = allowedEvents.contains(IntersectionEvents.SWITCH)
        val allowedStop = allowedEvents.contains(IntersectionEvents.STOP)
        withContext(Dispatchers.Main) {
            _allowStart.value = allowedStart
            _allowStop.value = allowedStop
            _allowSwitch.value = allowedSwitch
        }
        logger.info { "determineAllowed:start:${allowedStart}" }
        logger.info { "determineAllowed:stop:${allowedStop}" }
        logger.info { "determineAllowed:switch:${allowedSwitch}" }
    }

    suspend fun setupIntersection() {
        trafficIntersection.setupIntersection()
        CoroutineScope(Dispatchers.Default).launch {
            trafficIntersection.stopped.collect {
                viewModelScope.launch {
                    trafficIntersection.stopped()
                }
            }
        }
        trafficIntersection.state.collect { toState ->
            viewModelScope.launch {
                determineAllowed()
            }
        }
        viewModelScope.launch {
            determineAllowed()
        }
    }

    suspend fun startSystem() {
        logger.info { "startSystem" }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            snapshot.enter {
                trafficIntersection.startSystem()
            }
            snapshot.apply()
        } catch (x: Throwable) {
            logger.error("startSystem:$x", x)
            snapshot.dispose()
            throw x
        }
    }

    suspend fun stopSystem() {
        logger.info { "stopSystem" }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            snapshot.enter {
                trafficIntersection.stopSystem()
            }
            snapshot.apply()
        } catch (x: Throwable) {
            logger.error("stopSystem:$x", x)
            snapshot.dispose()
            throw x
        }
    }

    suspend fun switch() {
        logger.info { "switch" }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            snapshot.enter {
                trafficIntersection.switch()
            }
        } catch (x: Throwable) {
            logger.error("switch:$x", x)
            snapshot.dispose()
            throw x
        }
    }
}