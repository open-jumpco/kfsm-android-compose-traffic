package com.example.kfsm.compose.trafficlight


import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kfsm.compose.trafficlight.ui.theme.Amber
import com.example.kfsm.compose.trafficlight.ui.theme.KFSMComposeTrafficLightTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val intersectionModel = TrafficIntersectionModel()
    private val intersectionViewModel = TrafficIntersectionViewModel(intersectionModel)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KFSMComposeTrafficLightTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Intersection(intersectionViewModel)
                }
            }
        }
    }
}

@Composable
fun LightView(
    modifier: Modifier,
    interior: Color,
    state: State<Boolean>
) {
    val color = if (state.value) interior else Color.Black
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(color, size.minDimension / 4, center)
    }
}

@Composable
fun TrafficLightView(modifier: Modifier, viewModel: TrafficLightViewModel) {
    Column(
        modifier.drawBehind { drawRect(Color.DarkGray) },
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val lightModifier = Modifier.fillMaxHeight(1f)
            .aspectRatio(1f)
            .weight(1f)
            .padding(8.dp)
        Text(
            text = viewModel.name,
            modifier = lightModifier,
            color = Color.White,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        LightView(
            lightModifier,
            Color.Red,
            viewModel.red.observeAsState(false)
        )
        LightView(
            lightModifier,
            Amber,
            viewModel.amber.observeAsState(false)
        )
        LightView(
            lightModifier,
            Color.Green,
            viewModel.green.observeAsState(false)
        )
    }
}

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
            .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "State: ${state.name}, Active: ${viewModel.currentName}",
            modifier = Modifier.padding(4.dp)
        )
    }
    Row(
        Modifier
            .padding(8.dp)
            .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "CT: ${viewModel.cycleTime}, CWT: ${viewModel.cycleWaitTime}, AT: ${viewModel.amberTimeout}",
            modifier = Modifier.padding(4.dp),
            overflow = TextOverflow.Visible
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
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        viewModel.trafficLightData.forEach {
                            val trafficLightData = it.observeAsState(it.value!!)
                            TrafficLightView(
                                Modifier
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


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {

    KFSMComposeTrafficLightTheme {
        Text(text = "Traffic Light")
    }
}

