package com.example.kfsm.compose.trafficlight

import io.jumpco.open.kfsm.async.asyncStateMachine

enum class TrafficLightStates {
    RED,
    AMBER,
    GREEN
}

enum class TrafficLightEvents {
    STOP,
    GO
}

interface TrafficLight {
    val name: String
    val amberTimeout: Long
    fun setNotifyStopped(receiver: suspend () -> Unit)
    fun setNotifyStateChange(receiver: suspend (newState: TrafficLightStates) -> Unit)
    suspend fun stopped()
    suspend fun switchRed(on: Boolean)
    suspend fun switchAmber(on: Boolean)
    suspend fun switchGreen(on: Boolean)
    suspend fun stateChanged(toState: TrafficLightStates)
}

class TrafficLightFSM(context: TrafficLight) {
    companion object {
        private val definition = asyncStateMachine(
            TrafficLightStates.values().toSet(),
            TrafficLightEvents.values().toSet(), TrafficLight::class
        ) {
            initialState { TrafficLightStates.RED }
            onStateChange { _, toState ->
                stateChanged(toState)
            }
            whenState(TrafficLightStates.RED) {
                onEvent(TrafficLightEvents.GO to TrafficLightStates.GREEN) {
                    switchRed(false)
                    switchGreen(true)
                }
                onEvent(TrafficLightEvents.STOP) {
                    switchGreen(false)
                    switchAmber(false)
                    switchRed(true)
                }
            }
            whenState(TrafficLightStates.AMBER) {
                timeout(TrafficLightStates.RED, { amberTimeout }) {
                    switchAmber(false)
                    switchRed(true)
                    stopped()
                }
                onEvent(TrafficLightEvents.STOP) {
                }
            }
            whenState(TrafficLightStates.GREEN) {
                onEvent(TrafficLightEvents.STOP to TrafficLightStates.AMBER) {
                    switchGreen(false)
                    switchAmber(true)
                }
            }
        }.build()
    }

    private val fsm = definition.create(context)

    suspend fun start() {
        fsm.sendEvent(TrafficLightEvents.GO)
    }

    suspend fun stop() {
        fsm.sendEvent(TrafficLightEvents.STOP)
    }
}