package com.example.kfsm.compose.trafficlight


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
import com.example.kfsm.compose.trafficlight.ui.theme.KFSMComposeTrafficLightTheme

private val intersectionModel = TrafficIntersectionModel()
private var intersectionViewModel = TrafficIntersectionViewModel(intersectionModel)
private var portraitMode: MutableState<Boolean> = mutableStateOf(false)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KFSMComposeTrafficLightTheme {
                Scaffold {
                    portraitMode.value = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
                    Surface(color = MaterialTheme.colors.background) {
                        Intersection(intersectionViewModel, portraitMode.value)
                    }
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {

    KFSMComposeTrafficLightTheme {
        Scaffold {
            portraitMode.value = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
            Surface(color = MaterialTheme.colors.background) {
                Intersection(intersectionViewModel, portraitMode.value)
            }
        }
    }
}

