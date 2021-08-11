package com.example.kfsm.compose.trafficlight

class TrafficIntersectionModel : TrafficIntersectionImplementation(
    listOf(
        TrafficLightModel("1"),
        TrafficLightModel("2")/*,
        TrafficLightModel("3")*/
    )
) {
    override fun get(name: String): TrafficLightModel {
        return super.get(name) as TrafficLightModel
    }
}