package com.example.kfsm.compose.traffic.fsm

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TrafficIntersectionEventHandler : TrafficIntersectionContext {
    val amberTimeout: Long
    val state: StateFlow<IntersectionStates>
    val stopped: SharedFlow<Long>
    val currentName: String
    val current: TrafficLightContext
    val listOrder: List<String>
    val trafficLights: List<TrafficLightEventHandler>
    fun get(name: String): TrafficLightEventHandler
    fun changeCycleTime(value: Long)
    fun changeCycleWaitTime(value: Long)
    fun changeAmberTimeout(value: Long)
    fun addTrafficLight(name: String, trafficLight: TrafficLightEventHandler)
    fun allowedEvents(): Set<IntersectionEvents>
    suspend fun setupIntersection() // Called by creator as part of initialising system. Not called by FSM
    suspend fun stopped() // indicates the light has stopped. Not called by FSM
    suspend fun startTrafficLight(name: String) // Not called by FSM
    suspend fun startSystem() // Not called by FSM
    suspend fun stopSystem() // Not called by FSM
    suspend fun switch() // Not called by FSM
}