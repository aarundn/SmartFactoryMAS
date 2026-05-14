package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SystemStatusCard(state: MultiMachineState, modifier: Modifier = Modifier) {
    val alerts  = listOfNotNull(
        if (state.upstreamConflict)   "Upstream conflict"  else null,
        if (state.downstreamConflict) "Downstream delay"   else null)
    val active  = 3   // machines always active (AMA, AMS, AMV)

    val overallStatus = when {
        state.amsStatus == MachineStatus.ANOMALY      -> "Degraded"   to Color(0xFFDC2626)
        state.amsStatus == MachineStatus.RESCHEDULING -> "Recovering" to Color(0xFFF59E0B)
        state.amsStatus == MachineStatus.DONE         -> "Synced"     to Color(0xFF16A34A)
        else                                          -> "Stable"     to Color(0xFF16A34A)
    }

    Column(modifier = modifier
        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
        .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("System Status", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("● ${overallStatus.first}", color = overallStatus.second, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusTile("${alerts.size}", "ALERTS",  if (alerts.isEmpty()) SurfaceContainerLow else Color(0xFFFEE2E2),
                if (alerts.isEmpty()) OnSurfaceVariant else Color(0xFFB91C1C))
            StatusTile("$active", "ACTIVE",  SurfaceContainerLow, OnSurfaceVariant)
            StatusTile("${state.messages.size}", "MESSAGES", SurfaceContainerLow, Primary)
        }
    }
}

@Composable
private fun StatusTile(value: String, label: String, bg: Color, fg: Color) {
    Column(
        modifier = Modifier.background(bg, RoundedCornerShape(8.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = fg, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = fg.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}