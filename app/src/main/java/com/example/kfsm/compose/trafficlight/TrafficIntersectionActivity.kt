package com.example.kfsm.compose.trafficlight

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
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
        contentPadding = PaddingValues(4.dp)
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
        val allowStart = model.allowStart.observeAsState(false)
        StateButton(
            "Start",
            allowStart.value,
            model,
            coroutineScope
        ) {
            startSystem()
        }
        val allowStop = model.allowStop.observeAsState(false)
        StateButton(
            "Stop",
            allowStop.value,
            model,
            coroutineScope
        ) {
            stopSystem()
        }
        val allowSwitch = model.allowSwitch.observeAsState(false)
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
fun Intersection(viewModel: TrafficIntersectionViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    Column {
        val state = viewModel.intersectionState.observeAsState(IntersectionStates.STOPPED)
        IntersectionState(state.value, viewModel)
        when (configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                Column {
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
                        viewModel.trafficLightData.forEach {
                            val trafficLightData = it.observeAsState(it.value!!)
                            TrafficLightView(
                                Modifier
                                    .aspectRatio(0.4f, true)
                                    .weight(1f)
                                    .padding(16.dp),
                                trafficLightData.value
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
            }
            else -> {
                Row {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight()
                            .padding(16.dp)
                            .drawWithContent { drawRect(Color.LightGray) },
                        horizontalArrangement = Arrangement.SpaceEvenly,
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
                    IntersectionControls(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f),
                        viewModel,
                        coroutineScope
                    )
                }
            }
        }
    }
}
