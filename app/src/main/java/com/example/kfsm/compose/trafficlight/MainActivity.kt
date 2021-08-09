package com.example.kfsm.compose.trafficlight


import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kfsm.compose.trafficlight.ui.theme.Amber
import com.example.kfsm.compose.trafficlight.ui.theme.KFSMComposeTrafficLightTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val intersectionModel = TrafficIntersectionModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KFSMComposeTrafficLightTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Intersection(intersectionModel)
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
fun TrafficLightView(modifier: Modifier, model: TrafficLightModel) {
    Column(
        modifier.drawBehind { drawRect(Color.DarkGray) },
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val lightModifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .weight(1f)
            .padding(8.dp)
        LightView(
            lightModifier,
            Color.Red,
            model.red.observeAsState(false)
        )
        LightView(
            lightModifier,
            Amber,
            model.amber.observeAsState(false)
        )
        LightView(
            lightModifier,
            Color.Green,
            model.green.observeAsState(false)
        )
    }
}

@Composable
fun StateButton(
    name: String,
    allow: Boolean,
    model: TrafficIntersectionModel,
    coroutineScope: CoroutineScope,
    onChange: suspend TrafficIntersectionModel.() -> Unit
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
    model: TrafficIntersectionModel,
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
            start()
        }
        val allowStop = model.allowStop.observeAsState(false)
        StateButton(
            "Stop",
            allowStop.value,
            model,
            coroutineScope
        ) {
            stop()
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
fun IntersectionState(state: IntersectionStates, model: TrafficIntersectionModel) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "State: ${state.name}", modifier = Modifier.padding(4.dp))
    }
    Row(
        Modifier
            .padding(8.dp)
            .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "LC: ${model.leftCycleTime}, RC: ${model.rightCycleTime}, CT: ${model.cycleWaitTime}, LAT: ${model.leftAmberTimeout}, RAT: ${model.rightAmberTimeout}",
            modifier = Modifier.padding(4.dp),
            overflow = TextOverflow.Visible
        )
    }
}

@Composable
fun Intersection(model: TrafficIntersectionModel) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    Column {
        val state = model.intersectionState.observeAsState(IntersectionStates.STOPPED)
        IntersectionState(state.value, model)
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
                        TrafficLightView(
                            Modifier
                                .fillMaxSize()
                                .weight(0.8f)
                                .padding(16.dp),
                            model.left
                        )
                        TrafficLightView(
                            Modifier
                                .fillMaxSize()
                                .weight(0.8f)
                                .padding(16.dp),
                            model.right
                        )
                    }
                    IntersectionControls(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .weight(0.2f),
                        model,
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
                        TrafficLightView(
                            Modifier
                                .fillMaxWidth(0.3f)
                                .fillMaxHeight()
                                .weight(1f)
                                .padding(16.dp),
                            model.left
                        )
                        TrafficLightView(
                            Modifier
                                .fillMaxWidth(0.3f)
                                .fillMaxHeight()
                                .weight(1f)
                                .padding(16.dp),
                            model.right
                        )
                    }
                    IntersectionControls(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f),
                        model,
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

