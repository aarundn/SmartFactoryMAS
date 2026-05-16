package com.example.smartfactorymas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AcademicDashboard(
    somResult: BatchResultEvent?,
    sopResult: BatchResultEvent?,
    modifier: Modifier = Modifier
) {
    var isMultiMachine by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Dashboard Header & Toggles ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Academic Benchmark Results",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    "Compare Reactivity (Table 4.6) and Stability (Table 4.7)",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }

            // 1M vs MM Toggle
            Row(
                modifier = Modifier
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                TabButton("Single-Machine (1M)", !isMultiMachine) { isMultiMachine = false }
                TabButton("Multi-Machine (MM)", isMultiMachine) { isMultiMachine = true }
            }
        }

        if (somResult != null && sopResult != null) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Panel: Table 4.6 (Reactivity)
                ChartCard(
                    title = "Table 4.6: Reactivity & Execution Time",
                    subtitle = "Execution time across anomaly positions (Lower is better)",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    ReactivityGroupedBarChart(somResult, sopResult, isMultiMachine)
                }

                // Right Panel: Table 4.7 (Stability)
                ChartCard(
                    title = "Table 4.7: Schedule Stability",
                    subtitle = "Impact of CBM on overall job tardiness (100% Scale)",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    StabilityStackedBarChart(somResult, sopResult, isMultiMachine)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Run both SOM and SOP batches to view comparison.", color = Color.Gray)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Table 4.6: Reactivity (Grouped Bar Chart)
// ---------------------------------------------------------------------------
@Composable
private fun ReactivityGroupedBarChart(
    somResult: BatchResultEvent,
    sopResult: BatchResultEvent,
    isMultiMachine: Boolean
) {
    val textMeasurer = rememberTextMeasurer()

    // Extract data based on architecture
    val somData = listOf(
        if (isMultiMachine) somResult.splits.debut.multiMs else somResult.splits.debut.singleMs,
        if (isMultiMachine) somResult.splits.milieu.multiMs else somResult.splits.milieu.singleMs,
        if (isMultiMachine) somResult.splits.fin.multiMs else somResult.splits.fin.singleMs
    )

    val sopData = listOf(
        if (isMultiMachine) sopResult.splits.debut.multiMs else sopResult.splits.debut.singleMs,
        if (isMultiMachine) sopResult.splits.milieu.multiMs else sopResult.splits.milieu.singleMs,
        if (isMultiMachine) sopResult.splits.fin.multiMs else sopResult.splits.fin.singleMs
    )

    val maxY = maxOf(1.0, (somData + sopData).maxOrNull() ?: 1.0) * 1.2
    val labels = listOf("Début", "Milieu", "Fin")

    Canvas(modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 30.dp, start = 40.dp, end = 20.dp)) {
        val width = size.width
        val height = size.height
        val groupWidth = width / 3f
        val barWidth = groupWidth * 0.3f
        val spacing = groupWidth * 0.1f

        // Draw Y-Axis Grid
        for (i in 0..4) {
            val y = height - (height * (i / 4f))
            val value = maxY * (i / 4f)
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "%.1f".format(value),
                topLeft = Offset(-35.dp.toPx(), y - 8.dp.toPx()),
                style = TextStyle(color = Color(0xFF64748B), fontSize = 10.sp)
            )
        }

        // Draw Bars
        for (i in 0..2) {
            val xCenter = (i * groupWidth) + (groupWidth / 2f)

            // SOM Bar (Indigo)
            val somHeight = (somData[i] / maxY) * height
            drawRoundRect(
                color = Color(0xFF6366F1),
                topLeft = Offset(xCenter - barWidth - (spacing/2), (height - somHeight).toFloat()),
                size = Size(barWidth, somHeight.toFloat()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // SOP Bar (Rose)
            val sopHeight = (sopData[i] / maxY) * height
            drawRoundRect(
                color = Color(0xFFF43F5E),
                topLeft = Offset(xCenter + (spacing/2), (height - sopHeight).toFloat()),
                size = Size(barWidth, sopHeight.toFloat()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // X-Axis Labels
            val labelLayout = textMeasurer.measure(labels[i], TextStyle(color = Color(0xFF334155), fontWeight = FontWeight.Bold))
            drawText(
                textMeasurer = textMeasurer,
                text = labels[i],
                topLeft = Offset(xCenter - (labelLayout.size.width / 2f), height + 10.dp.toPx()),
                style = TextStyle(color = Color(0xFF334155), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            )
        }
    }

    // Legend
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        LegendItem(Color(0xFF6366F1), "SOM (Safety First)")
        Spacer(Modifier.width(24.dp))
        LegendItem(Color(0xFFF43F5E), "SOP (Production First)")
    }
}

// ---------------------------------------------------------------------------
// Table 4.7: Stability (100% Stacked Bar Chart)
// ---------------------------------------------------------------------------
@Composable
private fun StabilityStackedBarChart(
    somResult: BatchResultEvent,
    sopResult: BatchResultEvent,
    isMultiMachine: Boolean
) {
    val textMeasurer = rememberTextMeasurer()

    val somMetrics = if (isMultiMachine) somResult.stability.multi else somResult.stability.single
    val sopMetrics = if (isMultiMachine) sopResult.stability.multi else sopResult.stability.single

    val strategies = listOf("Strategy SOM", "Strategy SOP")
    val data = listOf(somMetrics, sopMetrics)

    Canvas(modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 30.dp, start = 40.dp, end = 20.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width * 0.25f

        // Y-Axis
        for (i in 0..4) {
            val y = height - (height * (i / 4f))
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "${(i * 25)}%",
                topLeft = Offset(-35.dp.toPx(), y - 8.dp.toPx()),
                style = TextStyle(color = Color(0xFF64748B), fontSize = 10.sp)
            )
        }

        // Draw Stacked Bars
        for (i in 0..1) {
            val xCenter = if (i == 0) width * 0.33f else width * 0.66f
            val metrics = data[i]

            // Calculate heights based on 100%
            val stableH = (metrics.stable / 100f) * height
            val improvedH = (metrics.improved / 100f) * height
            val detH = (metrics.deteriorated / 100f) * height

            val startX = xCenter - (barWidth / 2f)
            var currentY = height

            // 1. Stable Segment (Slate)
            currentY -= stableH.toFloat()
            drawRect(Color(0xFF94A3B8), Offset(startX, currentY), Size(barWidth, stableH.toFloat()))

            // 2. Improved Segment (Emerald)
            currentY -= improvedH.toFloat()
            drawRect(Color(0xFF10B981), Offset(startX, currentY), Size(barWidth, improvedH.toFloat()))

            // 3. Deteriorated Segment (Red)
            currentY -= detH.toFloat()
            drawRect(Color(0xFFEF4444), Offset(startX, currentY), Size(barWidth, detH.toFloat()))

            // X-Axis Labels
            val labelLayout = textMeasurer.measure(strategies[i])
            drawText(
                textMeasurer = textMeasurer,
                text = strategies[i],
                topLeft = Offset(xCenter - (labelLayout.size.width / 2f), height + 10.dp.toPx()),
                style = TextStyle(color = Color(0xFF334155), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            )
        }
    }

    // Legend
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        LegendItem(Color(0xFFEF4444), "Détérioré")
        Spacer(Modifier.width(16.dp))
        LegendItem(Color(0xFF94A3B8), "Stable")
        Spacer(Modifier.width(16.dp))
        LegendItem(Color(0xFF10B981), "Amélioré")
    }
}

// --- Helpers ---
@Composable
private fun ChartCard(title: String, subtitle: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.White else Color.Transparent,
            contentColor = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)
        ),
        shape = RoundedCornerShape(6.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(2.dp) else ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = Color(0xFF475569))
    }
}