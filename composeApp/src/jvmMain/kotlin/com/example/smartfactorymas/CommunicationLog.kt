package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay

@Composable
fun CommunicationLog(
    logs: List<LogEvent>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    DashboardCard(modifier = modifier) {
        Text("Multi-Agent Comm Log", color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs) { event ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Agent badge
                        Text(
                            text = "[${event.agent}]",
                            color = agentColor(event.agent),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.width(6.dp))
                        
                        // Step badge (if present)
                        event.step?.let {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF334155), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("Step $it", color = Color.White.copy(0.7f), fontSize = 9.sp)
                            }
                            Spacer(Modifier.width(6.dp))
                        }

                        // Message
                        Text(
                            text = event.msg,
                            color = levelColor(event.level),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

fun agentColor(agent: String): Color = when (agent) {
    "AMS"  -> Color(0xFF60A5FA)   // Blue-400
    "AMC"  -> Color(0xFFFBBF24)   // Amber-400
    "ASRH" -> Color(0xFF34D399)   // Emerald-400
    "SYS"  -> Color(0xFF94A3B8)   // Slate-400
    else   -> if (agent.startsWith("ARH")) Color(0xFFA78BFA) else Color(0xFFCBD5E1)
}

fun levelColor(level: String): Color = when (level) {
    "warn"  -> Color(0xFFFBBF24)
    "error" -> Color(0xFFF87171)
    else    -> Color(0xFFCBD5E1)
}
