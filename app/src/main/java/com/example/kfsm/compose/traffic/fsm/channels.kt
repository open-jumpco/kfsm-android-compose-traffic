package com.example.kfsm.compose.traffic.fsm

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect

fun <T> sendToChannel(
    channel: ReceiveChannel<T>,
    flow: MutableStateFlow<T>,
    coroutineScope: CoroutineScope
) {
    while (true) {
        coroutineScope.async {
            flow.emit(channel.receive())
        }
    }
}

fun <T> sendToChannel(
    channel: ReceiveChannel<T>,
    flow: MutableSharedFlow<T>,
    coroutineScope: CoroutineScope
) {
    while (true) {
        coroutineScope.async {
            flow.emit(channel.receive())
        }
    }
}

fun <T> sharedFlowToChannel(
    flow: SharedFlow<T>,
    channel: SendChannel<T>,
    coroutineScope: CoroutineScope
) {
    coroutineScope.async {
        flow.collect {
            channel.send(it)
        }
    }
}