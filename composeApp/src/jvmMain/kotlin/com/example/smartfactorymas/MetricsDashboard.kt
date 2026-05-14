// --- MetricsDashboard.kt ---
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetricsDashboard(f1: Float, f2: Float, globalF: Float, chosenArh: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard("Avg Prod Tardiness", "f1", "%.1f".format(f1), Primary, false)
        MetricCard("CBM Delay", "f2", "%.1f".format(f2), Tertiary, false)
        MetricCard("Global Obj Score", "f", "%.2f".format(globalF), Primary, true)
    }
}

@Composable
private fun MetricCard(title: String, symbol: String, value: String, valueColor: Color, isMaster: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceContainerLowest, RoundedCornerShape(8.dp)).border(if(isMaster) 2.dp else 1.dp, if(isMaster) Primary else OutlineVariant, RoundedCornerShape(8.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(symbol, color = OnSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Text(value, color = valueColor, fontSize = if(isMaster) 28.sp else 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}