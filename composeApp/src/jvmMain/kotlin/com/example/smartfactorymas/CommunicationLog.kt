// --- CommunicationLog.kt ---
package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock

@Composable
fun CommunicationLog(logs: List<LogEvent>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) { delay(100); listState.animateScrollToItem(logs.size - 1) } }

    Column(modifier = modifier.background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(12.dp)).fillMaxHeight()) {
        Row(modifier = Modifier.fillMaxWidth().background(SurfaceContainerLow, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Terminal, null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Multi-Agent System Log", color = OnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(SurfaceBright).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { event ->
                val agentCol = when {
                    event.agent == "AMC" || event.agent.contains("SYS") -> Error
                    event.agent == "ASRH" -> Secondary
                    event.agent.startsWith("ARH") -> PrimaryFixed
                    else -> Primary
                }
                Row {
                    Text("[${System.currentTimeMillis().toTime()}]", color = OnSurfaceVariant, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(text = "${event.agent}:", color = agentCol, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.width(6.dp))
                    Text(event.msg, color = OnSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                }
            }
        }
    }
}
fun Long.toTime(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}