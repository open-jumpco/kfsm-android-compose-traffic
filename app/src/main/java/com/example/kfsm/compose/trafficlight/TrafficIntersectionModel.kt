package com.example.kfsm.compose.trafficlight

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class TrafficIntersectionModel(
    private val intersectionImpl: TrafficIntersectionImplementation =
        TrafficIntersectionImplementation(
            listOf(
                TrafficLightModel("1"),
                TrafficLightModel("2")
            )
        )
) : ViewModel(), TrafficIntersection by intersectionImpl {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val intersectionFSM = TrafficIntersectionFSM(this)
    private val _intersectionState = MutableLiveData(intersectionFSM.currentState)
    val intersectionState: LiveData<IntersectionStates>
        get() = _intersectionState

    private val _allowStart = MutableLiveData(false)
    val allowStart: LiveData<Boolean> = _allowStart
    private val _allowSwitch = MutableLiveData(false)
    val allowSwitch: LiveData<Boolean> = _allowSwitch
    private val _allowStop = MutableLiveData(false)
    val allowStop: LiveData<Boolean> = _allowStop
    private var stateReceiver: (suspend (newState: IntersectionStates) -> Unit)? = null

    val amberTimeout: Long
        get() = current.amberTimeout

    init {
        GlobalScope.launch(Dispatchers.Main) {
            determineAllowed()
        }

        // Glue to connect TrafficIntersectionFSM and TrafficLightFSM events and to ensure they execute in the correct coroutine context
        intersectionImpl.setNotifyStopped {
            logger.info("stopped")
            GlobalScope.launch(Dispatchers.Main) {
                intersectionFSM.stopped()
            }
        }
        intersectionImpl.setNotifyStateChange { toState ->
            logger.info { "stateChanged:$toState" }
            GlobalScope.launch(Dispatchers.Main) {
                determineAllowed()
                _intersectionState.value = toState
                stateReceiver?.invoke(toState)
            }
        }
    }

    private fun determineAllowed() {
        val allowedEvents = intersectionFSM.allowedEvents()
        _allowStart.value = allowedEvents.contains(IntersectionEvents.START)
        _allowStop.value = allowedEvents.contains(IntersectionEvents.STOP)
        _allowSwitch.value = allowedEvents.contains(IntersectionEvents.SWITCH)
        logger.info { "determineAllowed:start:${_allowStart.value}" }
        logger.info { "determineAllowed:stop:${_allowStop.value}" }
        logger.info { "determineAllowed:switch:${_allowSwitch.value}" }
    }

    override fun get(name: String): TrafficLightModel {
        return intersectionImpl.get(name) as TrafficLightModel
    }

    suspend fun startSystem() {
        logger.info("startSystem")
        intersectionFSM.start()
    }

    suspend fun stopSystem() {
        logger.info("stopSystem")
        intersectionFSM.stop()
    }

    suspend fun switch() {
        logger.info("switch")
        intersectionFSM.switch()
    }
}