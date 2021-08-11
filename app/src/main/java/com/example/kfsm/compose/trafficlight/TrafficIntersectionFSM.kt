package com.example.kfsm.compose.trafficlight

import io.jumpco.open.kfsm.async.asyncStateMachine
import mu.KotlinLogging

enum class IntersectionStates {
    STOPPING,
    WAITING,
    GOING,
    NEXT,
    STOPPED
}

enum class IntersectionEvents {
    SWITCH,
    STOPPED,
    STOP,
    START
}

interface TrafficIntersection {
    val cycleTime: Long
    val cycleWaitTime: Long
    fun setNotifyStateChange(receiver: suspend (newState: IntersectionStates) -> Unit)
    fun setNotifyStopped(receiver: suspend () -> Unit)
    fun addTrafficLight(name: String, trafficLight: TrafficLight)
    val currentName: String
    val current: TrafficLight
    val listOrder: List<String>
    fun get(name: String): TrafficLight
    fun changeCycleTime(value: Long)
    fun changeCycleWaitTime(value: Long)
    suspend fun stateChanged(toState: IntersectionStates)
    suspend fun start()
    suspend fun stop()
    suspend fun next()
}

class TrafficIntersectionFSM(context: TrafficIntersection) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val definition = asyncStateMachine(
            IntersectionStates.values().toSet(),
            IntersectionEvents.values().toSet(),
            TrafficIntersection::class
        ) {
            initialState { IntersectionStates.STOPPED }
            onStateChange { _, toState ->
                stateChanged(toState)
            }
            whenState(IntersectionStates.STOPPED) {
                onEvent(IntersectionEvents.START to IntersectionStates.GOING) {
                    start()
                }
                onEvent(IntersectionEvents.STOPPED) {
                }
            }
            whenState(IntersectionStates.GOING) {
                timeout(IntersectionStates.STOPPING, { cycleTime }) {
                    stop()
                }
                onEvent(IntersectionEvents.SWITCH to IntersectionStates.STOPPING) {
                    stop()
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.STOPPED) {
                    stop()
                }
            }
            whenState(IntersectionStates.STOPPING) {
                onEvent(IntersectionEvents.STOPPED to IntersectionStates.WAITING) {
                }
                onEvent(IntersectionEvents.SWITCH to IntersectionStates.WAITING) {
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.STOPPED) {
                    stop()
                }
            }
            whenState(IntersectionStates.WAITING) {
                onEntry { _, _, _ ->
                    logger.info("WAITING:$cycleWaitTime")
                }
                timeout(IntersectionStates.GOING, { cycleWaitTime }) {
                    next()
                    start()
                }
                onEvent(IntersectionEvents.SWITCH) {
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.STOPPED) {
                    stop()
                }
            }
        }.build()
    }

    private val fsm = definition.create(context)
    val currentState: IntersectionStates
        get() = fsm.currentState

    suspend fun startIntersection() = fsm.sendEvent(IntersectionEvents.START)
    suspend fun stopIntersection() = fsm.sendEvent(IntersectionEvents.STOP)
    suspend fun switchIntersection() = fsm.sendEvent(IntersectionEvents.SWITCH)
    suspend fun stopped() = fsm.sendEvent(IntersectionEvents.STOPPED)
    fun allowedEvents() = fsm.allowed()
}

