package com.example.kfsm.compose.trafficlight

import mu.KotlinLogging

open class TrafficIntersectionImplementation(
    lights: List<TrafficLight>,
) : TrafficIntersection {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val trafficLights = mutableMapOf<String, TrafficLight>()
    private val stateMachines = mutableMapOf<String, TrafficLightFSM>()
    private val order = mutableListOf<String>()
    private val intersectionFSM = TrafficIntersectionFSM(this)
    private var _currentName: String
    private var _current: TrafficLight
    private var stoppedReceiver: (suspend () -> Unit)? = null
    private var _cycleWaitTime: Long = 1000L
    private var _cycleTime: Long = 5000L
    private var stateNotifier: (suspend (newState: IntersectionStates) -> Unit)? = null
    private var _amberTimeout: Long = 2000L

    init {
        require(lights.isNotEmpty()) { "At least one light is required" }
        _current = lights[0]
        _currentName = _current.name

        lights.forEach {
            it.changeAmberTimeout(amberTimeout)
            addTrafficLight(it.name, it)
            order.add(it.name)
        }
    }

    val amberTimeout: Long
        get() = _amberTimeout

    fun changeAmberTimeout(value: Long) {
        _amberTimeout = value
        trafficLights.values.forEach {
            it.changeAmberTimeout(value)
        }
    }

    override val listOrder: List<String>
        get() = order

    override fun get(name: String): TrafficLight {
        return trafficLights[name] ?: error("expected trafficLight:$name")
    }

    override val cycleTime: Long
        get() = _cycleTime

    override val currentName: String
        get() = _currentName

    override val current: TrafficLight
        get() = _current

    override val cycleWaitTime: Long
        get() = _cycleWaitTime
    val currentState: IntersectionStates
        get() = intersectionFSM.currentState

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
        logger.info { "$currentState:$currentName:start()" }
        fsm.start()
    }

    override suspend fun stop() {
        val fsm = stateMachines[currentName]
        requireNotNull(fsm) { "expected stateMachine for $currentName" }
        logger.info { "$currentState:$currentName:stop()" }
        fsm.stop()
    }

    override suspend fun off() {
        val fsm = stateMachines[currentName]
        requireNotNull(fsm) { "expected stateMachine for $currentName" }
        logger.info { "$currentState:$currentName:off()" }
        fsm.off()
    }

    override suspend fun next() {
        val oldName = currentName
        var index = order.indexOf(currentName) + 1
        if (index >= order.size) {
            index = 0
        }
        _currentName = order[index]
        logger.info { "$currentState:$oldName -> $currentName" }
    }

    suspend fun startSystem() {
        logger.info("startSystem")
        intersectionFSM.startIntersection()
    }

    suspend fun stopSystem() {
        logger.info("stopSystem")
        intersectionFSM.stopIntersection()
    }

    suspend fun switch() {
        logger.info("switch")
        intersectionFSM.switchIntersection()
    }

    suspend fun stopped() {
        logger.info("stopped")
        intersectionFSM.stopped()
    }

    fun allowedEvents(): Set<IntersectionEvents> = intersectionFSM.allowedEvents()

}