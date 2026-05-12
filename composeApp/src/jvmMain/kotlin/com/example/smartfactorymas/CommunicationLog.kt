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
import kotlinx.coroutines.delay

@Composable
fun CommunicationLog(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            delay(100) // Small delay to let layout pass finish
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    DashboardCard(modifier = modifier) {
        Text("Multi-Agent Comm Log", color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp)) // Slate900 exact
                .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs) { log ->
                    val color = when {
                        log.contains("CRITICAL") || log.contains("ERROR") -> Error
                        log.contains("CBM") || log.contains("Strategy") -> Secondary
                        else -> Color(0xFFCBD5E1) // Slate300 exact
                    }
                    Text(
                        text = log,
                        color = color,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
