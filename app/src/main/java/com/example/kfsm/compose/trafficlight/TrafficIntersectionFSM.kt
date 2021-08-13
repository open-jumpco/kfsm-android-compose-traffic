package com.example.kfsm.compose.trafficlight

import io.jumpco.open.kfsm.async.asyncStateMachine
import mu.KotlinLogging

enum class IntersectionStates {
    STOPPING,
    WAITING,
    GOING,
    NEXT,
    WAITING_STOPPED,
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
    suspend fun off()
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
                onEntry { _, _, _ ->
                    logger.info("STOPPED")
                }
                onEvent(IntersectionEvents.START to IntersectionStates.GOING) {
                    logger.info { "STOPPED:START" }
                    start()
                }
                onEvent(IntersectionEvents.STOPPED) {
                    logger.info { "STOPPED:STOPPED" }
                }
            }
            whenState(IntersectionStates.GOING) {
                onEntry { _, _, _ ->
                    logger.info("GOING:$cycleTime")
                }
                timeout(IntersectionStates.STOPPING, { cycleTime }) {
                    logger.info { "GOING:timeout" }
                    stop()
                }
                onEvent(IntersectionEvents.SWITCH to IntersectionStates.STOPPING) {
                    logger.info { "GOING:SWITCH" }
                    stop()
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.WAITING_STOPPED) {
                    logger.info { "GOING:STOP" }
                    stop()
                }
            }
            whenState(IntersectionStates.STOPPING) {
                onEntry { _, _, _ ->
                    logger.info("STOPPING")
                }
                onEvent(IntersectionEvents.STOPPED to IntersectionStates.WAITING) {
                    logger.info { "STOPPED" }
                }
                onEvent(IntersectionEvents.SWITCH) {
                    logger.info { "SWITCH" }
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.WAITING_STOPPED) {
                    logger.info { "STOP" }
                }
            }
            whenState(IntersectionStates.WAITING) {
                onEntry { _, _, _ ->
                    logger.info("WAITING:$cycleWaitTime")
                }
                timeout(IntersectionStates.GOING, { cycleWaitTime }) {
                    logger.info { "WAITING:timeout" }
                    next()
                    start()
                }
                onEvent(IntersectionEvents.SWITCH) {
                    logger.info { "WAITING:SWITCH" }
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.WAITING_STOPPED) {
                    logger.info { "WAITING:STOP" }
                }
            }
            whenState(IntersectionStates.WAITING_STOPPED) {
                onEntry { _, _, _ ->
                    logger.info("WAITING_STOPPED:${cycleWaitTime / 2}")
                }
                timeout(IntersectionStates.STOPPED, { cycleWaitTime / 2 }) {
                    logger.info { "WAITING_STOPPED:timeout" }
                    off()
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

