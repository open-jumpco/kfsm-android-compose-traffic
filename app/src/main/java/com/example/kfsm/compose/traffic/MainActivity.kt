package com.example.kfsm.compose.traffic


import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import com.example.kfsm.compose.traffic.fsm.TrafficIntersectionService
import com.example.kfsm.compose.traffic.fsm.TrafficLightService
import com.example.kfsm.compose.traffic.theme.KFSMComposeTrafficLightTheme
import kotlinx.coroutines.*

private val intersectionModel = TrafficIntersectionService(
    listOf(
        TrafficLightService("1"),
        TrafficLightService("2"),
        TrafficLightService("3")
    )
)
private var intersectionViewModel = TrafficIntersectionViewModel(intersectionModel)
private var portraitMode: MutableState<Boolean> = mutableStateOf(false)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.Default).launch {
            intersectionViewModel.setupIntersection()
        }
        setContent {
            portraitMode.value =
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
            MainWindow()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainWindow() {
    KFSMComposeTrafficLightTheme {
        Scaffold {
            Surface(color = MaterialTheme.colors.background) {
                Intersection(intersectionViewModel, portraitMode.value)
            }
        }
    }

}

