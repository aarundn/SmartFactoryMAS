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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 🌟 ألوان الآلات المخصصة 🌟
private val AMA_TOP = Color(0xFFFDE68A)    // Amber Light
private val AMA_BOTTOM = Color(0xFFFCD34D) // Amber Dark
private val AMA_BORDER = Color(0xFFD97706) // Amber Border
private val AMA_TEXT = Color(0xFF78350F)   // Amber Text (Dark Brown)

private val AMS_TOP = Color(0xFFC7D2FE)    // Indigo Light
private val AMS_BOTTOM = Color(0xFFA5B4FC) // Indigo Dark
private val AMS_BORDER = Color(0xFF818CF8) // Indigo Border
private val AMS_TEXT = Color(0xFF312E81)   // Indigo Text

private val AMV_TOP = Color(0xFF6EE7B7)    // Green Light
private val AMV_BOTTOM = Color(0xFF34D399) // Green Dark
private val AMV_BORDER = Color(0xFF10B981) // Green Border
private val AMV_TEXT = Color(0xFF064E3B)   // Green Text

@Composable
fun MultiMachineGanttChart(
    state: MultiMachineState,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    .border(1.dp, SurfaceVariant).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Multi-Machine Factory Timeline",
                    color = OnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                val stepLabel = when (state.currentStep) {
                    0 -> "STEP 0: STEADY STATE"
                    1 -> "STEP 1: ANOMALY DETECTED"
                    2 -> "STEP 2: AMS RESCHEDULING"
                    3 -> "STEP 3: UPSTREAM NEGOTIATION (M_MSG)"
                    else -> "STEP 4: GLOBAL SYNC COMPLETE"
                }
                Text(
                    stepLabel,
                    color = OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // ── Canvas ──────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val labelW = 80.dp.toPx()
                    val axisH = 32.dp.toPx()
                    val laneH = 72.dp.toPx()
                    val msgZoneH = 100.dp.toPx() // مساحة واسعة للرسائل
                    val chartW = size.width - labelW

                    val allBlocks = state.amaSchedule + state.amsSchedule + state.amvSchedule + state.amvOriginalSchedule

                    val maxTime = maxOf(
                        160.0,
                        allBlocks.maxOfOrNull { it.startTime + it.duration } ?: 160.0) + 10.0
                    val scale = chartW / maxTime.toFloat()

                    // ── Time axis ─────────────────────────────────────
                    drawRect(Color(0xFFF3F4F6), size = Size(size.width, axisH))
                    drawLine(Color(0xFFD1D5DB), Offset(0f, axisH), Offset(size.width, axisH), 1.5f)

                    val tickInterval = if (maxTime > 250) 40 else 50
                    for (t in 0..maxTime.toInt() step tickInterval) {
                        if (t == 0) continue
                        val x = labelW + t * scale
                        drawLine(
                            Color(0xFFE5E7EB), Offset(x, axisH), Offset(x, size.height), 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                        val tl = textMeasurer.measure(
                            "$t",
                            TextStyle(
                                color = Color(0xFF374151),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        drawText(
                            tl,
                            topLeft = Offset(x - tl.size.width / 2f, (axisH - tl.size.height) / 2f)
                        )
                    }

                    // ── Lane Y positions ──────────────────────────────
                    val amaY = axisH
                    val msg1Y = axisH + laneH
                    val amsY = msg1Y + msgZoneH
                    val msg2Y = amsY + laneH
                    val amvY = msg2Y + msgZoneH

                    // ── Draw each machine row ─────────────────────────
                    drawMachineRow(
                        this, textMeasurer, labelW, amaY, laneH, chartW, scale,
                        state.amaId, state.amaSchedule, state.amaStatus,
                        topColor = AMA_TOP, bottomColor = AMA_BOTTOM, borderColor = AMA_BORDER, textColor = AMA_TEXT
                    )

                    drawMachineRow(
                        this, textMeasurer, labelW, amsY, laneH, chartW, scale,
                        "AMS (Subject)", state.amsSchedule, state.amsStatus,
                        topColor = AMS_TOP, bottomColor = AMS_BOTTOM, borderColor = AMS_BORDER, textColor = AMS_TEXT
                    )

                    drawMachineRow(
                        this, textMeasurer, labelW, amvY, laneH, chartW, scale,
                        state.amvId, state.amvSchedule, state.amvStatus,
                        topColor = AMV_TOP, bottomColor = AMV_BOTTOM, borderColor = AMV_BORDER, textColor = AMV_TEXT,
                        originalSchedule = if (state.currentStep >= 4 && state.downstreamConflict) state.amvOriginalSchedule else emptyList()
                    )

                    // ── Anomaly line ──────────────────────────────────
                    if (state.currentStep >= 1) {
                        val anomalyX = labelW + (state.anomalyTime * scale).toFloat()
                        drawLine(
                            Color(0xFFDC2626),
                            Offset(anomalyX, axisH),
                            Offset(anomalyX, size.height),
                            2f
                        )
                        val aLabel = textMeasurer.measure(
                            "t=${state.anomalyTime.toInt()}",
                            TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                        val boxW = aLabel.size.width + 10f;
                        val boxH = aLabel.size.height + 6f
                        drawRoundRect(
                            Color(0xFFDC2626),
                            topLeft = Offset((anomalyX - boxW / 2f).coerceAtLeast(labelW), axisH - boxH),
                            size = Size(boxW, boxH), cornerRadius = CornerRadius(3.dp.toPx())
                        )
                        drawText(aLabel, topLeft = Offset((anomalyX - boxW / 2f).coerceAtLeast(labelW) + 5f, axisH - boxH + 3f))
                    }

                    // ── M_MESSAGE arrows (UPWARD: AMS -> AMA) ──────────────────────────────
                    val mMessages = state.messages.filter { it.type == "M_MESSAGE" }
                    mMessages.forEachIndexed { idx, msg ->
                        val startX = labelW + (msg.originalTime * scale).toFloat()
                        val endX = labelW + (msg.requestedTime * scale).toFloat()
                        drawCurvedMessageArrow(
                            this, textMeasurer, startX, endX, msg1Y, msgZoneH, msg,
                            arrowColor = Color(0xFFD97706), bgColor = Color(0xFFFEF3C7), textColor = Color(0xFF92400E),
                            isUpward = true
                        )
                    }

                    // ── I_MESSAGE arrows (DOWNWARD: AMS -> AMV) ──────────────────────────────
                    val iMessages = state.messages.filter { it.type == "I_MESSAGE" }
                    iMessages.forEachIndexed { idx, msg ->
                        val startX = labelW + (msg.originalTime * scale).toFloat()
                        val endX = labelW + (msg.requestedTime * scale).toFloat()
                        drawCurvedMessageArrow(
                            this, textMeasurer, startX, endX, msg2Y, msgZoneH, msg,
                            arrowColor = Color(0xFF4B5A9C), bgColor = Color(0xFFDDE1FF), textColor = Color(0xFF001354),
                            isUpward = false
                        )
                    }
                }
            }

            // ── Step navigation ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canPrev = state.currentStep > 0 && !isRunning
                OutlinedButton(
                    onClick = onPrevStep,
                    shape = RoundedCornerShape(6.dp),
                    enabled = canPrev
                ) {
                    Text("< Previous Step", color = if (canPrev) OnSurfaceVariant else OutlineVariant)
                }
                val iterLabel = when (state.currentStep) {
                    4 -> "Iteration: 4 (Global Sync Complete)"
                    else -> "Iteration: ${state.currentStep}"
                }
                Text(iterLabel, color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                val canNext = state.currentStep < 4 && state.amsSchedule.isNotEmpty() && !isRunning
                OutlinedButton(
                    onClick = onNextStep,
                    shape = RoundedCornerShape(6.dp),
                    enabled = canNext
                ) {
                    Text("Next Step >", color = if (canNext) OnSurfaceVariant else OutlineVariant)
                }
            }
        }
    }
}

// ─── Canvas drawing helpers ─────────────────────────────────────────────────

private fun drawMachineRow(
    scope: DrawScope, measurer: androidx.compose.ui.text.TextMeasurer,
    labelW: Float, laneY: Float, laneH: Float, chartW: Float, scale: Float,
    machineId: String, blocks: List<TaskBlock>, status: MachineStatus,
    topColor: Color, bottomColor: Color, borderColor: Color, textColor: Color,
    originalSchedule: List<TaskBlock> = emptyList()
) = with(scope) {
    drawRoundRect(
        Color(0xFFF1F5F9).copy(alpha = 0.5f),
        topLeft = Offset(labelW, laneY), size = Size(chartW, laneH),
        cornerRadius = CornerRadius(8.dp.toPx())
    )

    val labelBg = when (status) {
        MachineStatus.ANOMALY, MachineStatus.CONFLICT -> Color(0xFFFEE2E2)
        MachineStatus.RESOLVED, MachineStatus.DONE -> Color(0xFFDCFCE7)
        MachineStatus.SHIFTED -> Color(0xFFEDE9FE)
        else -> Color(0xFFE0E3E5)
    }
    val labelText = when (status) {
        MachineStatus.STEADY -> "Steady State"
        MachineStatus.ANOMALY -> "⚠ Anomaly Detected"
        MachineStatus.RESCHEDULING -> "⟳ Rescheduling…"
        MachineStatus.CONFLICT -> "⚡ Conflict"
        MachineStatus.RESOLVED -> "✓ Resolved"
        MachineStatus.SHIFTED -> "↔ Shifted Schedule"
        MachineStatus.DONE -> "✓ Sync Complete"
    }

    drawRoundRect(
        labelBg, topLeft = Offset(0f, laneY + 4.dp.toPx()),
        size = Size(labelW - 4.dp.toPx(), laneH - 8.dp.toPx()),
        cornerRadius = CornerRadius(8.dp.toPx())
    )

    val idLayout = measurer.measure(
        machineId,
        TextStyle(color = OnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )
    drawText(idLayout, topLeft = Offset(8.dp.toPx(), laneY + 8.dp.toPx()))

    val stLayout = measurer.measure(
        labelText,
        TextStyle(
            color = when (status) {
                MachineStatus.ANOMALY, MachineStatus.CONFLICT -> Color(0xFFB91C1C)
                MachineStatus.RESOLVED, MachineStatus.DONE -> Color(0xFF15803D)
                MachineStatus.SHIFTED -> Color(0xFF6D28D9)
                else -> OnSurfaceVariant
            }, fontSize = 9.sp, fontWeight = FontWeight.SemiBold
        )
    )
    drawText(
        stLayout,
        topLeft = Offset(8.dp.toPx(), laneY + 8.dp.toPx() + idLayout.size.height + 4.dp.toPx())
    )

    originalSchedule.forEach { block ->
        val sx = labelW + (block.startTime * scale).toFloat()
        val bw = (block.duration * scale).toFloat().coerceAtLeast(2f)
        val top = laneY + 12.dp.toPx()
        val bh = laneH - 24.dp.toPx()
        drawRoundRect(
            Color(0xFFE5E7EB), topLeft = Offset(sx, top), size = Size(bw, bh),
            cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx())
        )
        val ol = measurer.measure("orig", TextStyle(color = Color(0xFFBFC3CC), fontSize = 9.sp))
        if (bw > ol.size.width + 4) drawText(
            ol,
            topLeft = Offset(sx + (bw - ol.size.width) / 2f, top + (bh - ol.size.height) / 2f)
        )
    }

    blocks.forEach { block ->
        val startPx = labelW + (block.startTime * scale).toFloat()
        val widthPx = (block.duration * scale).toFloat().coerceAtLeast(2f)
        val blockTop = laneY + 10.dp.toPx()
        val blockHeight = laneH - 20.dp.toPx()
        val cornerRad = CornerRadius(4.dp.toPx())

        val (tc, bc, bBorder, tCol) = when (block.type) {
            TaskType.CBM -> listOf(Color(0xFFFECDD3), Color(0xFFFDA4AF), Color(0xFFFB7185), Color(0xFF881337))
            TaskType.TBM -> listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF334155))
            TaskType.PRODUCTION -> listOf(topColor, bottomColor, borderColor, textColor)
        }

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.1f),
            topLeft = Offset(startPx, blockTop + 3.dp.toPx()),
            size = Size(widthPx, blockHeight),
            cornerRadius = cornerRad
        )

        drawRoundRect(
            brush = Brush.verticalGradient(listOf(tc, bc), startY = blockTop, endY = blockTop + blockHeight),
            topLeft = Offset(startPx, blockTop),
            size = Size(widthPx, blockHeight),
            cornerRadius = cornerRad
        )

        drawRoundRect(
            color = bBorder,
            topLeft = Offset(startPx, blockTop),
            size = Size(widthPx, blockHeight),
            cornerRadius = cornerRad,
            style = Stroke(1.dp.toPx())
        )

        val textLayout = measurer.measure(
            text = block.id,
            style = TextStyle(color = tCol, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )

        if (widthPx > textLayout.size.width + 4f) {
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

// 🌟 دالة رسم الأسهم المنحنية (Curved Arrows) الذكية 🌟
private fun drawCurvedMessageArrow(
    scope: DrawScope, measurer: androidx.compose.ui.text.TextMeasurer,
    startX: Float, endX: Float, zoneY: Float, zoneH: Float,
    msg: NegotiationMessage,
    arrowColor: Color, bgColor: Color, textColor: Color,
    isUpward: Boolean
) = with(scope) {
    val midY = zoneY + zoneH / 2f
    val topY = zoneY + 12.dp.toPx()
    val botY = zoneY + zoneH - 12.dp.toPx()
    val tipSize = 6.dp.toPx()

    // حساب نقاط البداية والنهاية بناءً على الاتجاه (من AMS إلى AMA أم من AMS إلى AMV)
    val startPtX = startX
    val startPtY = if (isUpward) botY else topY
    val endPtX = endX
    val endPtY = if (isUpward) topY + tipSize else botY - tipSize

    // رسم مسار بيزييه (Bezier Curve)
    val path = Path().apply {
        moveTo(startPtX, startPtY)
        cubicTo(
            startPtX, midY,
            endPtX, midY,
            endPtX, endPtY
        )
    }

    drawPath(
        path = path,
        color = arrowColor,
        style = Stroke(
            width = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            cap = StrokeCap.Round
        )
    )

    // رسم رأس السهم في النهاية (يشير للأعلى أو للأسفل)
    val arrowHead = Path().apply {
        if (isUpward) { // رأس السهم للأعلى لرسالة AMA
            moveTo(endX, topY)
            lineTo(endX - tipSize, topY + tipSize)
            lineTo(endX + tipSize, topY + tipSize)
            close()
        } else { // رأس السهم للأسفل لرسالة AMV
            moveTo(endX, botY)
            lineTo(endX - tipSize, botY - tipSize)
            lineTo(endX + tipSize, botY - tipSize)
            close()
        }
    }
    drawPath(arrowHead, arrowColor)

    // صندوق نص الرسالة (Badge) يوضع في منتصف المسافة
    val line1 = "${msg.type}: ${msg.jobId}"
    val line2 = "t${msg.originalTime.toInt()} → t${msg.requestedTime.toInt()} ${if (msg.accepted) "✓" else "✗"}"
    val l1 = measurer.measure(line1, TextStyle(color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold))
    val l2 = measurer.measure(line2, TextStyle(color = textColor, fontSize = 9.sp))

    val pillW = maxOf(l1.size.width, l2.size.width) + 16f
    val pillH = l1.size.height + l2.size.height + 12f

    // وضع الصندوق في منتصف المسافة الأفقية للمنحنى
    val midX = (startX + endX) / 2f
    val pillX = (midX - pillW / 2f).coerceAtLeast(0f)
    val pillY = midY - pillH / 2f

    drawRoundRect(
        bgColor, topLeft = Offset(pillX, pillY), size = Size(pillW, pillH),
        cornerRadius = CornerRadius(6.dp.toPx())
    )
    drawRoundRect(
        arrowColor, topLeft = Offset(pillX, pillY), size = Size(pillW, pillH),
        cornerRadius = CornerRadius(6.dp.toPx()), style = Stroke(1.dp.toPx())
    )
    drawText(l1, topLeft = Offset(pillX + 8f, pillY + 6f))
    drawText(l2, topLeft = Offset(pillX + 8f, pillY + 6f + l1.size.height + 2f))
}