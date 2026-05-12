package com.example.smartfactorymas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun GanttChart(
    state: GanttState,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    isAutoRunning: Boolean,
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Title with selected ARH info
            val title = if (state.masOutput != null && state.masOutput.proposals.isNotEmpty()) {
                val prop = state.masOutput.proposals.getOrNull(state.selectedProposalIdx)
                val isChosen = prop?.arhId == state.masOutput.chosenArh
                "Schedule: ${prop?.arhId ?: ""}${if (isChosen) " ★ (Selected)" else ""}"
            } else {
                "Subject Machine (MS) Schedule"
            }

            Text(title, color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            val subtitle = when (state.currentStep) {
                0 -> "Initial Schedule (Table 4.1)"
                1 -> "Anomaly Detected — Negotiating..."
                3 -> "Computed Schedule (Real Calculation)"
                else -> ""
            }
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            } else {
                Spacer(Modifier.height(16.dp))
            }

            // Canvas
            val textMeasurer = rememberTextMeasurer()
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .background(SurfaceBright, RoundedCornerShape(8.dp))
                    .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
                    .padding(top = 32.dp, bottom = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxTime = max(160.0, state.schedule.maxOfOrNull { it.endMax } ?: 160.0) + 10.0
                    val pxPerUnit = size.width / maxTime.toFloat()
                    
                    val tracksToDraw = if (state.tracks.isNotEmpty()) state.tracks else listOf(state.schedule)
                    val numTracks = tracksToDraw.size
                    
                    // Distribute tracks vertically
                    val trackHeight = 36.dp.toPx()
                    val totalTracksHeight = numTracks * trackHeight
                    val spacing = if (numTracks > 1) (size.height - totalTracksHeight - 40.dp.toPx()) / (numTracks - 1) else 0f
                    
                    // X-Axis at the bottom
                    val axisY = size.height - 12.dp.toPx()
                    drawLine(OutlineVariant, Offset(0f, axisY), Offset(size.width, axisY), 1.5f)
                    drawLine(OutlineVariant, Offset(size.width - 8f, axisY - 4f), Offset(size.width, axisY), 1.5f)
                    drawLine(OutlineVariant, Offset(size.width - 8f, axisY + 4f), Offset(size.width, axisY), 1.5f)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "t",
                        style = TextStyle(color = OnSurfaceVariant, fontSize = 12.sp),
                        topLeft = Offset(size.width - 4f, axisY + 4f)
                    )

                    // Real timing axis ticks (every 20 units)
                    val tickInterval = 20
                    for (t in 0..maxTime.toInt() step tickInterval) {
                        val x = (t * pxPerUnit).toFloat()
                        drawLine(OnSurfaceVariant, Offset(x, axisY - 3f), Offset(x, axisY + 3f), 1f)
                        drawLine(OutlineVariant.copy(alpha = 0.2f), Offset(x, 0f), Offset(x, axisY),
                            0.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                        
                        val textLayout = textMeasurer.measure("$t", TextStyle(color = OnSurfaceVariant, fontSize = 10.sp))
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(x - textLayout.size.width / 2f, axisY + 6f)
                        )
                    }

                    // Anomaly line and RUL Zone
                    if (state.currentStep >= 1) {
                        val anomalyPx = (8.0 * pxPerUnit).toFloat()
                        
                        // RUL Zone
                        val cbmBlock = state.schedule.find { it.type == TaskType.CBM }
                        if (cbmBlock != null) {
                            val rulEndPx = (cbmBlock.startTime * pxPerUnit).toFloat()
                            drawRect(
                                color = Error.copy(alpha = 0.08f),
                                topLeft = Offset(anomalyPx, 0f),
                                size = Size(rulEndPx - anomalyPx, axisY)
                            )
                            drawText(
                                textMeasurer = textMeasurer,
                                text = "RUL Zone",
                                style = TextStyle(color = Error.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                topLeft = Offset(anomalyPx + 4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        // Zigzag Signal d'anomalie
                        val zigzagPath = Path().apply {
                            moveTo(anomalyPx, 0f)
                            lineTo(anomalyPx + 8f, 15f)
                            lineTo(anomalyPx - 8f, 30f)
                            lineTo(anomalyPx, 45f)
                            lineTo(anomalyPx, axisY)
                        }
                        drawPath(zigzagPath, Error, style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))))
                        
                        // Arrow head
                        val arrowSize = 5.dp.toPx()
                        val arrowPath = Path().apply {
                            moveTo(anomalyPx, axisY)
                            lineTo(anomalyPx - arrowSize, axisY - arrowSize)
                            lineTo(anomalyPx + arrowSize, axisY - arrowSize)
                            close()
                        }
                        drawPath(arrowPath, Error)
                        
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "Signal d'anomalie",
                            style = TextStyle(color = Error, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            topLeft = Offset(anomalyPx + 12f, 16f)
                        )
                    }

                    // Draw each track
                    tracksToDraw.forEachIndexed { trackIdx, trackBlocks ->
                        val trackY = if (numTracks == 1) (size.height - trackHeight) / 2f else trackIdx * (trackHeight + spacing)
                        
                        // Track label (0), (1), etc.
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "(${trackIdx}) MS",
                            style = TextStyle(color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            topLeft = Offset(2f, trackY + trackHeight / 2 - 8f)
                        )
                        
                        // Track base line
                        drawLine(OutlineVariant, Offset(40.dp.toPx(), trackY + trackHeight), Offset(size.width, trackY + trackHeight), 1f)

                        // Blocks for this track
                        trackBlocks.forEach { block ->
                            val startPx = (block.startTime * pxPerUnit).toFloat()
                            val widthPx = (block.duration * pxPerUnit).toFloat()

                            // Fuzzy range shading (lighter band for min/max)
                            if (block.startMin != block.startMax || block.endMin != block.endMax) {
                                val fuzzyStartPx = (block.startMin * pxPerUnit).toFloat()
                                val fuzzyEndPx = (block.endMax * pxPerUnit).toFloat()
                                drawRect(
                                    color = when (block.type) {
                                        TaskType.CBM -> Tertiary.copy(alpha = 0.15f)
                                        else -> Primary.copy(alpha = 0.15f)
                                    },
                                    topLeft = Offset(fuzzyStartPx, trackY),
                                    size = Size(fuzzyEndPx - fuzzyStartPx, trackHeight)
                                )
                            }

                            val blockColor = when (block.type) {
                                TaskType.PRODUCTION -> Primary
                                TaskType.CBM -> Tertiary
                                TaskType.TBM -> SurfaceContainerHighest
                            }

                            drawRect(blockColor, Offset(startPx, trackY), Size(widthPx, trackHeight))

                            // TBM crosshatch
                            if (block.type == TaskType.TBM) {
                                val hatchSpacing = 5.dp.toPx()
                                var ly = trackY + hatchSpacing
                                while (ly < trackY + trackHeight) {
                                    drawLine(OnSurfaceVariant.copy(0.3f), Offset(startPx, ly), Offset(startPx + widthPx, ly), 0.8f)
                                    ly += hatchSpacing
                                }
                                var lx = startPx + hatchSpacing
                                while (lx < startPx + widthPx) {
                                    drawLine(OnSurfaceVariant.copy(0.3f), Offset(lx, trackY), Offset(lx, trackY + trackHeight), 0.8f)
                                    lx += hatchSpacing
                                }
                            }
                            
                            // CBM diagonal stripes
                            if (block.type == TaskType.CBM) {
                                val hatchSpacing = 6.dp.toPx()
                                var d = -trackHeight
                                while (d < widthPx) {
                                    val startX = maxOf(startPx, startPx + d)
                                    val startY = if (d < 0) trackY - d else trackY
                                    val endX = minOf(startPx + widthPx, startPx + d + trackHeight)
                                    val endY = if (d + trackHeight > widthPx) trackY + (startPx + widthPx - (startPx + d)) else trackY + trackHeight
                                    
                                    if (startX <= endX) {
                                        drawLine(OnTertiary.copy(0.5f), Offset(startX, startY), Offset(endX, endY), 1.dp.toPx())
                                    }
                                    d += hatchSpacing
                                }
                            }

                            // Border
                            drawRect(SurfaceContainerLowest.copy(0.6f), Offset(startPx, trackY), Size(widthPx, trackHeight), style = Stroke(1.dp.toPx()))

                            // Label
                            val textColor = when (block.type) {
                                TaskType.TBM -> OnSurface
                                TaskType.PRODUCTION -> OnPrimary
                                TaskType.CBM -> OnTertiary
                            }
                            val textLayout = textMeasurer.measure(block.id, TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            if (widthPx > textLayout.size.width + 4f) {
                                drawText(
                                    textLayoutResult = textLayout,
                                    topLeft = Offset(
                                        startPx + (widthPx - textLayout.size.width) / 2f,
                                        trackY + (trackHeight - textLayout.size.height) / 2f
                                    )
                                )
                            }

                            // Deadline marker (small red triangle above block)
                            if (block.deadline != null && block.type == TaskType.PRODUCTION) {
                                val dlPx = (block.deadline * pxPerUnit).toFloat()
                                if (dlPx >= startPx && dlPx <= startPx + widthPx + 20) {
                                    drawLine(Error.copy(0.6f), Offset(dlPx, trackY - 8f), Offset(dlPx, trackY), 1.5f)
                                }
                            }
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(Primary, "Production Job")
                Spacer(Modifier.width(16.dp))
                LegendItem(SurfaceContainerHighest, "TBM Activity")
                Spacer(Modifier.width(16.dp))
                LegendItem(Tertiary, "CBM (Corrective)")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, color = OnSurfaceVariant, fontSize = 11.sp)
    }
}
