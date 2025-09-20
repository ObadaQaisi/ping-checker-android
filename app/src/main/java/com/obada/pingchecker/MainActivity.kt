package com.obada.pingchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
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
    val iconRes: Int? = null,
)

val sites = listOf(
    Site("Google", "google.com", R.drawable.ic_google),
    Site("GitHub", "github.com"),
    Site("Facebook", "facebook.com"),
    Site("YouTube", "youtube.com", R.drawable.ic_youtube),
    Site("Amazon", "aws.amazon.com"),
    Site("Outlook", "outlook.com"),
    Site("Offline Check","Offline.Sample"),
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PingCheckerTheme {
                SitesScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen() {
    val scope = rememberCoroutineScope()
    val statusMap = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(Unit) {
        sites.forEach { statusMap[normalizeUrl(it.url)] = "Unknown" }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Website status") },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        val jobs = sites.map { site ->
                            val url = normalizeUrl(site.url)
                            async {
                                statusMap[url] = "Checking…"
                                statusMap[url] = checkHttpOnce(url)
                            }
                        }
                        jobs.awaitAll()
                    }
                },
                text = { Text("Check all") },
                icon = { Icon(Icons.Default.Refresh, null) }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sites, key = { it.url }) { site ->
                val url = normalizeUrl(site.url)
                val status = statusMap[url] ?: "Unknown"
                SiteRow(
                    name = site.name,
                    url = url,
                    status = status,
                    iconRes = site.iconRes
                )
            }
        }
    }
}
@Composable
fun SiteRow(name: String, url: String, status: String, iconRes: Int?) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(status = status)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(12.dp))
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = "$name icon",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            }

        }
    }
}

@Composable
fun StatusDot(status: String) {
    val c = when {
        status.startsWith("Online") -> Color(0xFF2ECC71)
        status.startsWith("Checking") -> MaterialTheme.colorScheme.primary
        status == "Unknown" -> Color(0xFF95A5A6)
        else -> Color(0xFFE74C3C)
    }
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(MaterialTheme.shapes.small)
            .background(c)
    )
}
