package com.obada.pingchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.obada.pingchecker.ui.theme.PingCheckerTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

enum class Screen {Main, Second}
var screen by mutableStateOf(Screen.Main)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PingCheckerTheme {
                when(screen) {
                    Screen.Main -> MainScreen ()
                    Screen.Second -> SecondScreen ()
                }
            }
        }
    }
}
@Composable
fun Greeting(name: String) {
    Text("Hello $name")
}
@Composable
fun Clicker() {
    var count by remember { mutableIntStateOf(0) }

    Column {
        Text("Clicked $count times")
        Button(onClick = { count++ }) {
            Text("Click me")
        }
    }
}

@Composable
fun SwitchScreen() {
    fun switch(){

        if(screen == Screen.Main)
            screen = Screen.Second
        else screen = Screen.Main
    }

    Column {
        Button(onClick = { switch() }) {
            Text("change screen")
        }
    }
}
@Composable
fun MainScreen() {
    Column {
        Greeting("Obada")
        Clicker()
        SwitchScreen()
        Text("Screen: $screen")
    }
}
@Composable
fun SecondScreen() {
    Column{
        SwitchScreen()
        Text("Screen: $screen")
    }
}