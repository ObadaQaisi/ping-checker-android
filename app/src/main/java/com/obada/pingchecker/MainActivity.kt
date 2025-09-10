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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

enum class Screen {Main, Second}
var screen by mutableStateOf(Screen.Main)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                PingCheckerTheme {
                    when (screen) {
                        Screen.Main -> MainScreen()
                        Screen.Second -> SecondScreen()
                    }
                    SwitchScreen()
                    Text("Screen: $screen")
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
        screen = if(screen == Screen.Main)
            Screen.Second
        else Screen.Main
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
    }
}
@Composable
fun SecondScreen() {
    Column{
        PingButton()
    }
}

suspend fun checkHttpOnce(host: String): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(if (host.startsWith("http")) host else "https://$host")
        .head()
        .build()

    val result = try {
        client.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) "Online ✅" else "Offline ❌"
        }
    } catch (_: Exception) {
        "Offline ❌"
    }
    result
}

@Composable
fun PingButton() {
    var status by remember { mutableStateOf("Unknown") }
    val scope = rememberCoroutineScope()

    Column {
        Text("Result: $status")
        Button(onClick = { scope.launch { status = checkHttpOnce("google.com") } }) {
            Text("Check Google")
        }
    }
}