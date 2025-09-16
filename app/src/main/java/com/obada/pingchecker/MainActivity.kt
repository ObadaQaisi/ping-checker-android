package com.obada.pingchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obada.pingchecker.ui.theme.PingCheckerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
data class Site(
    val name: String,
    val url: String,
)

val sites = listOf(
    Site("Google", "google.com"),
    Site("GitHub", "github.com"),
    Site("Stack Overflow", "stackoverflow.com"),
    Site("YouTube", "youtube.com"),
    Site("Should be offline", "example.invalid") // guaranteed offline/DNS error
)

private val http = OkHttpClient.Builder()
    .callTimeout(5, TimeUnit.SECONDS)
    .build()

private fun normalizeUrl(hostOrUrl: String): String =
    if (hostOrUrl.startsWith("http")) hostOrUrl else "https://$hostOrUrl"

suspend fun checkHttpOnce(hostOrUrl: String): String = withContext(Dispatchers.IO) {
    val url = normalizeUrl(hostOrUrl)

    fun requestHead() = Request.Builder().url(url).head().build()
    fun requestGet()  = Request.Builder().url(url).get().build()

    try {
        // Try HEAD first (fast), then GET fallback (some sites block HEAD)
        http.newCall(requestHead()).execute().use { resp ->
            if (resp.isSuccessful || (resp.code in 300..399)) return@withContext "Online ✅"
        }
        http.newCall(requestGet()).execute().use { resp ->
            return@withContext if (resp.isSuccessful || (resp.code in 300..399)) "Online ✅"
            else "Offline ❌ (HTTP ${resp.code})"
        }
    } catch (e: java.net.UnknownHostException) {
        "Offline ❌ (DNS error)"
    } catch (e: java.net.SocketTimeoutException) {
        "Offline ❌ (Timeout)"
    } catch (_: Exception) {
        "Offline ❌"
    }
}

// ---- UI ---------------------------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PingCheckerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SitesScreen()
                }
            }
        }
    }
}

@Composable
fun SitesScreen() {
    val scope = rememberCoroutineScope()

    // Map URL -> status string. Keeps Compose state OUT of your data model.
    val statusMap = remember { mutableStateMapOf<String, String>() }

    // Initialize unknowns once
    LaunchedEffect(Unit) {
        sites.forEach { statusMap[normalizeUrl(it.url)] = "Unknown" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Website status", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    scope.launch {
                        val jobs = sites.map { site ->
                            val url = normalizeUrl(site.url)
                            async {
                                statusMap[url] = "Checking…"
                                val result = checkHttpOnce(url)
                                statusMap[url] = result
                            }
                        }
                        jobs.awaitAll()
                    }
                }
            ) { Text("Check all") }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sites, key = { it.url }) { site ->
                val url = normalizeUrl(site.url)
                val status = statusMap[url] ?: "Unknown"

                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(site.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(2.dp))
                            Text(url)
                            Spacer(Modifier.height(6.dp))
                            Text("Status: $status")
                        }
                        Spacer(Modifier.width(12.dp))
                        val rowScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                rowScope.launch {
                                    statusMap[url] = "Checking…"
                                    statusMap[url] = checkHttpOnce(url)
                                }
                            }
                        ) { Text("Check") }
                    }
                }
            }
        }
    }
}
