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
fun MetricsDashboard(
    f1: Float,
    f2: Float,
    globalF: Float,
    chosenArh: String?,
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier = modifier) {
        Text("KPI Scorecards", color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = "Avg Prod Tardiness (f1)",
                value = "%.1f".format(f1),
                trend = if (f1 > 0f) "↓" else null,
                isMaster = false
            )
            
            MetricCard(
                label = "CBM Delay (f2)",
                value = "%.1f".format(f2),
                trend = null,
                isMaster = false
            )
            
            MetricCard(
                label = "Global Obj Score (f)",
                value = "%.2f".format(globalF),
                trend = null,
                isMaster = true
            )
            
            if (chosenArh != null) {
                MetricCard(
                    label = "Selected Worker",
                    value = chosenArh,
                    trend = null,
                    isMaster = false
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    trend: String?,
    isMaster: Boolean
) {
    val bgColor = if (isMaster) PrimaryFixed else SurfaceContainer
    val borderColor = if (isMaster) PrimaryContainer else OutlineVariant
    val labelColor = if (isMaster) OnPrimaryFixed else OnSurfaceVariant
    val valueColor = if (isMaster) OnPrimaryFixed else OnSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = labelColor, fontSize = 13.sp, fontWeight = if (isMaster) FontWeight.SemiBold else FontWeight.Medium)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = valueColor, fontSize = if (isMaster) 16.sp else 18.sp, fontWeight = FontWeight.Bold)
            if (trend != null) {
                Spacer(Modifier.width(8.dp))
                // Using a manual green color since Emerald500 is gone from Theme.kt
                Text(trend, color = Color(0xFF10B981), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
