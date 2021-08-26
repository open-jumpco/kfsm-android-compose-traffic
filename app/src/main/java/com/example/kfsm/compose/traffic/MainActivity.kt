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
private var portraitMode: MutableState<Boolean> = mutableStateOf(true)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.Default).launch {
            intersectionViewModel.setupIntersection()
        }
        setContent {
            MainWindow()
            portraitMode.value =
                Configuration.ORIENTATION_PORTRAIT == LocalConfiguration.current.orientation
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

