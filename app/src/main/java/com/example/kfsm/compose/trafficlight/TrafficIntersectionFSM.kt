package com.example.kfsm.compose.trafficlight

import io.jumpco.open.kfsm.async.asyncStateMachine

enum class IntersectionStates {
    LEFT_STOPPING,
    LEFT_WAITING,
    LEFT_GOING,
    RIGHT_STOPPING,
    RIGHT_WAITING,
    RIGHT_GOING,
    STOPPED
}

enum class IntersectionEvents {
    SWITCH,
    STOPPED,
    STOP,
    START
}

interface TrafficIntersection {
    val leftCycleTime: Long
    val rightCycleTime: Long
    val cycleWaitTime: Long
    fun setNotifyStateChange(receiver: suspend (newState: IntersectionStates) -> Unit)
    fun setNotifyStopped(receiver: suspend () -> Unit)
    suspend fun stateChanged(toState: IntersectionStates)
    suspend fun goRight()
    suspend fun goLeft()
    suspend fun stopLeft()
    suspend fun stopRight()
}

class TrafficIntersectionFSM(context: TrafficIntersection) {
    companion object {
        val definition = asyncStateMachine(
            IntersectionStates.values().toSet(),
            IntersectionEvents.values().toSet(),
            TrafficIntersection::class
        ) {
            initialState { IntersectionStates.STOPPED }
            onStateChange { _, toState ->
                stateChanged(toState)
            }
            whenState(IntersectionStates.STOPPED) {
                onEvent(IntersectionEvents.START to IntersectionStates.LEFT_GOING) {
                    stopRight()
                    goLeft()
                }
                onEvent(IntersectionEvents.STOPPED) {
                }
            }
            whenState(IntersectionStates.LEFT_GOING) {
                timeout(IntersectionStates.LEFT_STOPPING, { leftCycleTime }) {
                    stopLeft()
                }
                onEvent(IntersectionEvents.SWITCH to IntersectionStates.LEFT_STOPPING) {
                    stopLeft()
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.STOPPED) {
                    stopLeft()
                }
            }
            whenState(IntersectionStates.LEFT_STOPPING) {
                onEvent(IntersectionEvents.STOPPED to IntersectionStates.LEFT_WAITING) {
                }
                onEvent(IntersectionEvents.SWITCH to IntersectionStates.LEFT_WAITING) {
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.STOPPED) {
                    stopLeft()
                }
            }
            whenState(IntersectionStates.LEFT_WAITING) {
                timeout(IntersectionStates.RIGHT_GOING, { cycleWaitTime }) {
                    goRight()
                }
                onEvent(IntersectionEvents.SWITCH) {
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.STOPPED) {
                    stopLeft()
                }
            }
            whenState(IntersectionStates.RIGHT_GOING) {
                timeout(IntersectionStates.RIGHT_STOPPING, { rightCycleTime }) {
                    stopRight()
                }
                onEvent(IntersectionEvents.SWITCH to IntersectionStates.RIGHT_STOPPING) {
                    stopRight()
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.STOPPED) {
                    stopRight()
                }
            }
            whenState(IntersectionStates.RIGHT_STOPPING) {
                onEvent(IntersectionEvents.STOPPED to IntersectionStates.RIGHT_WAITING) {
                }
                onEvent(IntersectionEvents.SWITCH to IntersectionStates.RIGHT_WAITING) {
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.STOPPED) {
                }
            }
            whenState(IntersectionStates.RIGHT_WAITING) {
                timeout(IntersectionStates.LEFT_GOING, { cycleWaitTime }) {
                    goLeft()
                }
                onEvent(IntersectionEvents.SWITCH) {
                }
                onEvent(IntersectionEvents.STOP to IntersectionStates.STOPPED) {
                    stopRight()
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

class TrafficIntersectionImplementation(left: TrafficLight, right: TrafficLight) :
    TrafficIntersection {
    private val rightFSM = TrafficLightFSM(right)
    private val leftFSM = TrafficLightFSM(left)

    init {
        right.setNotifyStopped {
            rightFSM.stop()
            stoppedReceiver?.invoke()
        }
        left.setNotifyStopped {
            leftFSM.stop()
            stoppedReceiver?.invoke()
        }
    }

    var stoppedReceiver: (suspend () -> Unit)? = null
    var _leftCycleTime: Long = 10000L
    var _rightCycleTime: Long = 10000L
    var _cycleWaitTime: Long = 1000L
    override val leftCycleTime: Long
        get() = _leftCycleTime

    override val rightCycleTime: Long
        get() = _rightCycleTime
    override val cycleWaitTime: Long
        get() = _cycleWaitTime

    override fun setNotifyStopped(receiver: suspend () -> Unit) {
        stoppedReceiver = receiver
    }

    fun changeLeftCycleTime(value: Long) {
        _leftCycleTime = value
    }

    fun changeRightCycleTime(value: Long) {
        _rightCycleTime = value;
    }

    fun changeCycleWaitTime(value: Long) {
        _cycleWaitTime = value
    }

    var stateNotifier: (suspend (newState: IntersectionStates) -> Unit)? = null
    override fun setNotifyStateChange(receiver: suspend (newState: IntersectionStates) -> Unit) {
        stateNotifier = receiver
    }

    override suspend fun stateChanged(toState: IntersectionStates) {
        stateNotifier?.invoke(toState)
    }

    override suspend fun goRight() {
        rightFSM.start()
    }

    override suspend fun goLeft() {
        leftFSM.start()
    }

    override suspend fun stopLeft() {
        leftFSM.stop()
    }

    override suspend fun stopRight() {
        rightFSM.stop()
    }
}