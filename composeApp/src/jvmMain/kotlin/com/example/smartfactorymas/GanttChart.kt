package com.example.smartfactorymas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GanttChart(
    state: GanttState,
    anomalyTime: Double,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    isAutoRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    .border(1.dp, SurfaceVariant).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Subject Machine AMS Schedule",
                    color = OnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                val stepText = when (state.currentStep) {
                    0 -> "STEP 0: INITIAL SCHEDULE"
                    1 -> "STEP 1: ANOMALY DETECTED"
                    2 -> "STEP 2: NAIVE PACKING"
                    3 -> "STEP 3: OPTIMIZING..."
                    else -> "STEP 4: FINAL SHUFFLE"
                }
                Text(
                    stepText,
                    color = OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            val textMeasurer = rememberTextMeasurer()

            // Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tracksToDraw = state.tracks.ifEmpty { listOf(state.schedule) }
                    val numTracks = tracksToDraw.size
                    val allBlocks = tracksToDraw.flatten()
                    val maxTime = maxOf(160.0, allBlocks.maxOfOrNull { it.endMax } ?: 160.0) + 10.0

                    val axisHeight = 36.dp.toPx()
                    val chartScaleAxis = size.width / maxTime.toFloat()

                    // ==========================================
                    // 1. Timeline Axis Background & Ticks
                    // ==========================================
                    drawRect(color = Color(0xFFF3F4F6), size = Size(size.width, axisHeight))
                    drawLine(
                        Color(0xFFD1D5DB),
                        Offset(0f, axisHeight),
                        Offset(size.width, axisHeight),
                        1.5f
                    )

                    val tickInterval = if (maxTime > 250) 40 else 50
                    for (t in 0..maxTime.toInt() step tickInterval) {
                        if (t == 0) continue
                        val x = (t * chartScaleAxis).toFloat()

                        // Dashed Grid Lines
                        drawLine(
                            color = Color(0xFFE5E7EB),
                            start = Offset(x, axisHeight),
                            end = Offset(x, size.height),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )

                        // Tick Numbers
                        val textLayout = textMeasurer.measure(
                            "$t",
                            TextStyle(
                                color = Color(0xFF374151),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                x - textLayout.size.width / 2f,
                                (axisHeight - textLayout.size.height) / 2f
                            )
                        )
                    }

                    // ==========================================
                    // 2. Risk Zone & Anomaly Marker
                    // ==========================================
                    if (state.currentStep >= 1) {
                        val anomalyPx = (anomalyTime * chartScaleAxis).toFloat()
                        val rulMinPx = (state.rulMin * chartScaleAxis).toFloat()
                        val rulMaxPx = (state.rulMax * chartScaleAxis).toFloat()

                        // Risk Zone Background & Dashed Lines
                        drawRect(
                            color = Color(0xFFFEF2F2).copy(alpha = 0.5f),
                            topLeft = Offset(rulMinPx, axisHeight),
                            size = Size(rulMaxPx - rulMinPx, size.height - axisHeight)
                        )
                        drawLine(
                            Color(0xFFFECACA),
                            Offset(rulMinPx, axisHeight),
                            Offset(rulMinPx, size.height),
                            1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                        drawLine(
                            Color(0xFFFECACA),
                            Offset(rulMaxPx, axisHeight),
                            Offset(rulMaxPx, size.height),
                            1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        val riskText = textMeasurer.measure(
                            "RISK ZONE",
                            TextStyle(
                                color = Color(0xFFB91C1C),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        drawText(
                            textLayoutResult = riskText,
                            topLeft = Offset(rulMinPx + 8f, axisHeight + 8f)
                        )

                        // Anomaly Vertical Line
                        drawLine(
                            Color(0xFFDC2626),
                            Offset(anomalyPx, axisHeight),
                            Offset(anomalyPx, size.height),
                            2f
                        )

                        // Anomaly Label (Red Badge)
                        val anomalyStr = "Anomaly (t=${anomalyTime.toInt()})"
                        val anomalyText = textMeasurer.measure(
                            anomalyStr,
                            TextStyle(
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        val boxWidth = anomalyText.size.width + 12f
                        val boxHeight = anomalyText.size.height + 8f
                        drawRoundRect(
                            color = Color(0xFFDC2626),
                            topLeft = Offset(
                                maxOf(0f, anomalyPx - boxWidth / 2f),
                                axisHeight - boxHeight
                            ),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                        drawText(
                            textLayoutResult = anomalyText,
                            topLeft = Offset(
                                maxOf(0f, anomalyPx - boxWidth / 2f) + 6f,
                                axisHeight - boxHeight + 4f
                            )
                        )
                    }

                    // ==========================================
                    // 3. Tracks & Blocks (With Gradients & Shadows)
                    // ==========================================
                    val trackHeight = 56.dp.toPx()
                    val availableHeight = size.height - axisHeight
                    val spacing =
                        if (numTracks > 1) (availableHeight - (numTracks * trackHeight)) / (numTracks + 1) else availableHeight / 2 - trackHeight / 2

                    tracksToDraw.forEachIndexed { trackIdx, trackBlocks ->
                        val trackY = axisHeight + spacing + trackIdx * (trackHeight + spacing)

                        // Track Gray Background Band
                        drawRoundRect(
                            color = Color(0xFFF1F5F9).copy(alpha = 0.5f),
                            topLeft = Offset(16.dp.toPx(), trackY),
                            size = Size(size.width - 32.dp.toPx(), trackHeight),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )

                        trackBlocks.forEach { block ->
                            val startPx = (block.startTime * chartScaleAxis).toFloat()
                            val widthPx = (block.duration * chartScaleAxis).toFloat()

                            // Colors mapping identical to Tailwind/Images
                            val topColor: Color
                            val bottomColor: Color
                            val borderColor: Color
                            val textColor: Color

                            when (block.type) {
                                TaskType.PRODUCTION -> {
                                    topColor = Color(0xFFC7D2FE)    // Indigo 200
                                    bottomColor = Color(0xFFA5B4FC) // Indigo 300
                                    borderColor = Color(0xFF818CF8) // Indigo 400
                                    textColor = Color(0xFF312E81)   // Indigo 900
                                }

                                TaskType.CBM -> {
                                    topColor = Color(0xFFFECDD3)    // Rose 200
                                    bottomColor = Color(0xFFFDA4AF) // Rose 300
                                    borderColor = Color(0xFFFB7185) // Rose 400
                                    textColor = Color(0xFF881337)   // Rose 900
                                }

                                TaskType.TBM -> {
                                    topColor = Color(0xFFF1F5F9)    // Slate 100
                                    bottomColor = Color(0xFFE2E8F0) // Slate 200
                                    borderColor = Color(0xFFCBD5E1) // Slate 300
                                    textColor = Color(0xFF334155)   // Slate 700
                                }
                            }

                            val blockTop = trackY + 10.dp.toPx()
                            val blockHeight = trackHeight - 20.dp.toPx()
                            val cornerRad = CornerRadius(4.dp.toPx())

                            // A. Drop Shadow (Soft offset)
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.1f),
                                topLeft = Offset(startPx, blockTop + 3.dp.toPx()),
                                size = Size(widthPx, blockHeight),
                                cornerRadius = cornerRad
                            )

                            // B. Gradient Fill
                            val bgBrush = Brush.verticalGradient(
                                colors = listOf(topColor, bottomColor),
                                startY = blockTop,
                                endY = blockTop + blockHeight
                            )
                            drawRoundRect(
                                brush = bgBrush,
                                topLeft = Offset(startPx, blockTop),
                                size = Size(widthPx, blockHeight),
                                cornerRadius = cornerRad
                            )

                            // C. Solid Border
                            drawRoundRect(
                                color = borderColor,
                                topLeft = Offset(startPx, blockTop),
                                size = Size(widthPx, blockHeight),
                                cornerRadius = cornerRad,
                                style = Stroke(1.dp.toPx())
                            )

                            // D. Centered Bold Text
                            val textLayout = textMeasurer.measure(
                                text = block.id,
                                style = TextStyle(
                                    color = textColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )


                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(
                                    startPx + (widthPx - textLayout.size.width) / 2f,
                                    blockTop + (blockHeight - textLayout.size.height) / 2f
                                )
                            )
                        }
                    }
                }
            }

            // Stepper Footer
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onPreviousStep, shape = RoundedCornerShape(6.dp)) {
                    Text("< Previous Step", color = OnSurfaceVariant)
                }
                Text(
                    "Iteration: ${if (state.currentStep == 4) 43 else state.currentStep}",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(
                    onClick = onNextStep,
                    shape = RoundedCornerShape(6.dp),
                    enabled = state.currentStep < 4
                ) {
                    Text("Next Step >", color = OnSurfaceVariant)
                }
            }
        }
    }
}