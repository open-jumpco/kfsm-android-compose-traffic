package com.example.kfsm.compose.traffic

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.kfsm.compose.traffic.fsm.IntersectionStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun StateButton(
    name: String,
    allow: Boolean,
    model: TrafficIntersectionViewModel,
    coroutineScope: CoroutineScope,
    onChange: suspend TrafficIntersectionViewModel.() -> Unit
) {
    Button(
        onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.Default) {
                    onChange.invoke(model)
                }
            }
        },
        enabled = allow,
        contentPadding = PaddingValues(4.dp),
        elevation = ButtonDefaults.elevation()
    ) {
        Text(name)
    }
}

@Composable
fun IntersectionControls(
    modifier: Modifier,
    model: TrafficIntersectionViewModel,
    coroutineScope: CoroutineScope
) {
    Row(
        modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val allowStart = model.allowStart.collectAsState(false)
        StateButton(
            "Start",
            allowStart.value,
            model,
            coroutineScope
        ) {
            startSystem()
        }
        val allowStop = model.allowStop.collectAsState(false)
        StateButton(
            "Stop",
            allowStop.value,
            model,
            coroutineScope
        ) {
            stopSystem()
        }
        val allowSwitch = model.allowSwitch.collectAsState(false)
        StateButton(
            "Switch",
            allowSwitch.value,
            model,
            coroutineScope
        ) {
            switch()
        }
    }
}

@Composable
fun IntersectionState(state: IntersectionStates, viewModel: TrafficIntersectionViewModel) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Text(overflow = TextOverflow.Visible,
            text = buildAnnotatedString {
                append("State: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(state.name)
                }
                append(", Active: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(viewModel.currentName)
                }
                append("\nCycle time: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("${viewModel.cycleTime}ms")
                }
                append(", Wait time: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("${viewModel.cycleWaitTime}ms")
                }
                append(", Amber time: ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("${viewModel.amberTimeout}ms")
                }
            }
        )
    }
}

@Composable
fun Intersection(viewModel: TrafficIntersectionViewModel, portraitMode: Boolean) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    Column {
        val state = viewModel.intersectionState.collectAsState(IntersectionStates.STOPPED)
        if (portraitMode) {
            Column {
                IntersectionState(state.value, viewModel)
                Row(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .weight(0.8f)
                        .padding(16.dp)
                        .drawBehind { drawRect(Color.LightGray) },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    viewModel.trafficLights.forEach {
                        val trafficLightData = it
                        TrafficLightView(
                            Modifier
                                .aspectRatio(0.4f, true)
                                .weight(1f)
                                .padding(16.dp),
                            trafficLightData
                        )
                    }
                }
                IntersectionControls(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .weight(0.2f),
                    viewModel,
                    coroutineScope
                )
            }
        } else {
            Row {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    IntersectionState(state.value, viewModel)
                    IntersectionControls(
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .weight(1f),
                        viewModel,
                        coroutineScope
                    )
                }
                Row(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .padding(16.dp)
                        .drawBehind { drawRect(Color.LightGray) },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    viewModel.trafficLights.forEach {
                        TrafficLightView(
                            Modifier
                                .fillMaxWidth(0.3f)
                                .fillMaxHeight()
                                .weight(1f)
                                .padding(16.dp),
                            it
                        )
                    }
                }
            }
        }
    }
}
