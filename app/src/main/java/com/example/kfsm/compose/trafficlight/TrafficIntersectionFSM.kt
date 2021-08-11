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

    suspend fun start() = fsm.sendEvent(IntersectionEvents.START)
    suspend fun stop() = fsm.sendEvent(IntersectionEvents.STOP)
    suspend fun switch() = fsm.sendEvent(IntersectionEvents.SWITCH)
    suspend fun stopped() = fsm.sendEvent(IntersectionEvents.STOPPED)
    fun allowedEvents() = fsm.allowed()
}

class TrafficIntersectionImplementation(
    lights: List<TrafficLight>
) : TrafficIntersection {
    private val trafficLights = mutableMapOf<String, TrafficLight>()
    private val stateMachines = mutableMapOf<String, TrafficLightFSM>()
    private val order = mutableListOf<String>()
    var _currentName: String
    var _current: TrafficLight
    var stoppedReceiver: (suspend () -> Unit)? = null
    var _cycleWaitTime: Long = 1000L
    var _cycleTime: Long = 5000L
    var stateNotifier: (suspend (newState: IntersectionStates) -> Unit)? = null

    init {
        _current = (lights[0] ?: error("expected lights not empty"))
        _currentName = _current.name

        lights.forEach {
            addTrafficLight(it.name, it)
            order.add(it.name)
        }

    }

    override val listOrder: List<String>
        get() = order

    override fun get(name: String): TrafficLight {
        return trafficLights.get(name) ?: error("expected trafficLight:$name")
    }

    override val cycleTime: Long
        get() = _cycleTime

    override val currentName: String
        get() = _currentName

    override val current: TrafficLight
        get() = _current

    override val cycleWaitTime: Long
        get() = _cycleWaitTime

    override fun addTrafficLight(name: String, trafficLight: TrafficLight) {
        val fsm = TrafficLightFSM(trafficLight)
        trafficLight.setNotifyStopped {
            fsm.stop()
            stoppedReceiver?.invoke()
        }
        stateMachines[name] = fsm
        trafficLights[name] = trafficLight
    }

    override fun setNotifyStopped(receiver: suspend () -> Unit) {
        stoppedReceiver = receiver
    }

    override fun setNotifyStateChange(receiver: suspend (newState: IntersectionStates) -> Unit) {
        stateNotifier = receiver
    }

    override suspend fun stateChanged(toState: IntersectionStates) {
        stateNotifier?.invoke(toState)
    }

    override fun changeCycleTime(value: Long) {
        _cycleWaitTime = value
    }

    override fun changeCycleWaitTime(value: Long) {
        _cycleWaitTime = value
    }

    override suspend fun start() {
        val fsm = stateMachines[currentName]
        requireNotNull(fsm) { "expected stateMachine for $currentName" }
        fsm.start()
    }

    override suspend fun stop() {
        val fsm = stateMachines[currentName]
        requireNotNull(fsm) { "expected stateMachine for $currentName" }
        fsm.stop()
    }

    override suspend fun next() {
        var index = order.indexOf(currentName) + 1
        if (index >= order.size) {
            index = 0
        }
        _currentName = order[index]
    }
}