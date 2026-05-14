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

private val AMA_TOP    = Color(0xFF4B5A9C)   // Primary – upstream
private val AMA_BOTTOM = Color(0xFF8493C8)
private val AMS_TOP    = Color(0xFFFDA4AF)   // Tertiary – subject machine
private val AMS_BOTTOM = Color(0xFFFB7185)
private val AMV_TOP    = Color(0xFF6EE7B7)   // Green – downstream
private val AMV_BOTTOM = Color(0xFF34D399)
private val CBM_COLOR  = Color(0xFFFECACA)

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
                Text("Multi-Machine Factory Timeline", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val stepLabel = when (state.currentStep) {
                    0 -> "STEP 0: STEADY STATE"
                    1 -> "STEP 1: ANOMALY DETECTED"
                    2 -> "STEP 2: AMS RESCHEDULING"
                    3 -> "STEP 3: UPSTREAM NEGOTIATION (M_MSG)"
                    else -> "STEP 4: GLOBAL SYNC COMPLETE"
                }
                Text(stepLabel, color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            // ── Canvas ──────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val labelW   = 80.dp.toPx()
                    val axisH    = 32.dp.toPx()
                    val laneH    = 72.dp.toPx()
                    val msgZoneH = 56.dp.toPx()
                    val chartW   = size.width - labelW

                    val allBlocks = state.amaSchedule + state.amsSchedule + state.amvSchedule + state.amvOriginalSchedule
                    val maxTime   = maxOf(160.0, allBlocks.maxOfOrNull { it.endMax } ?: 160.0) + 10.0
                    val scale     = chartW / maxTime.toFloat()

                    // ── Time axis ─────────────────────────────────────
                    drawRect(Color(0xFFF3F4F6), size = Size(size.width, axisH))
                    drawLine(Color(0xFFD1D5DB), Offset(0f, axisH), Offset(size.width, axisH), 1.5f)

                    val tickInterval = if (maxTime > 250) 40 else 50
                    for (t in 0..maxTime.toInt() step tickInterval) {
                        if (t == 0) continue
                        val x = labelW + t * scale
                        drawLine(Color(0xFFE5E7EB), Offset(x, axisH), Offset(x, size.height), 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                        val tl = textMeasurer.measure("$t",
                            TextStyle(color = Color(0xFF374151), fontSize = 11.sp, fontWeight = FontWeight.Medium))
                        drawText(tl, topLeft = Offset(x - tl.size.width / 2f, (axisH - tl.size.height) / 2f))
                    }

                    // ── Lane Y positions ──────────────────────────────
                    val amaY  = axisH
                    val msg1Y = axisH + laneH
                    val amsY  = msg1Y + msgZoneH
                    val msg2Y = amsY + laneH
                    val amvY  = msg2Y + msgZoneH

                    // ── Draw each machine row ─────────────────────────
                    drawMachineRow(this, textMeasurer, labelW, amaY, laneH, chartW, scale,
                        state.amaId, state.amaSchedule, state.amaStatus,
                        topColor = AMA_TOP, bottomColor = AMA_BOTTOM, step = state.currentStep)

                    drawMachineRow(this, textMeasurer, labelW, amsY, laneH, chartW, scale,
                        "AMS (Subject)", state.amsSchedule, state.amsStatus,
                        topColor = AMS_TOP, bottomColor = AMS_BOTTOM, step = state.currentStep,
                        isCbmRow = true)

                    drawMachineRow(this, textMeasurer, labelW, amvY, laneH, chartW, scale,
                        state.amvId, state.amvSchedule, state.amvStatus,
                        topColor = AMV_TOP, bottomColor = AMV_BOTTOM, step = state.currentStep,
                        originalSchedule = if (state.currentStep >= 4 && state.downstreamConflict) state.amvOriginalSchedule else emptyList())

                    // ── Anomaly line ──────────────────────────────────
                    if (state.currentStep >= 1) {
                        val anomalyX = labelW + (state.anomalyTime * scale).toFloat()
                        drawLine(Color(0xFFDC2626), Offset(anomalyX, axisH), Offset(anomalyX, size.height), 2f)
                        val aLabel = textMeasurer.measure("t=${state.anomalyTime.toInt()}",
                            TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                        val boxW = aLabel.size.width + 10f; val boxH = aLabel.size.height + 6f
                        drawRoundRect(Color(0xFFDC2626),
                            topLeft = Offset((anomalyX - boxW / 2f).coerceAtLeast(labelW), axisH - boxH),
                            size = Size(boxW, boxH), cornerRadius = CornerRadius(3.dp.toPx()))
                        drawText(aLabel, topLeft = Offset((anomalyX - boxW / 2f).coerceAtLeast(labelW) + 5f, axisH - boxH + 3f))
                    }

                    // ── M_MESSAGE arrows (msg1 zone, between AMA and AMS) ──
                    val mMessages = state.messages.filter { it.type == "M_MESSAGE" }
                    mMessages.forEachIndexed { idx, msg ->
                        val msgX = labelW + (msg.requestedTime * scale).toFloat()
                        drawMessageArrow(this, textMeasurer, msgX, msg1Y, msgZoneH, msg,
                            arrowColor = Color(0xFF855316), bgColor = Color(0xFFFFDCBD),
                            textColor  = Color(0xFF683C00))
                    }

                    // ── I_MESSAGE arrows (msg2 zone, between AMS and AMV) ──
                    val iMessages = state.messages.filter { it.type == "I_MESSAGE" }
                    iMessages.forEachIndexed { idx, msg ->
                        val msgX = labelW + (msg.requestedTime * scale).toFloat()
                        drawMessageArrow(this, textMeasurer, msgX, msg2Y, msgZoneH, msg,
                            arrowColor = Color(0xFF4B5A9C), bgColor = Color(0xFFDDE1FF),
                            textColor  = Color(0xFF001354))
                    }

                    // ── "No messages yet" hints when step < 3 ────────
                    if (state.currentStep < 3) {
                        val hint1 = textMeasurer.measure("Material Flow ↓",
                            TextStyle(color = Color(0xFFBDC0C8), fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp))
                        drawText(hint1, topLeft = Offset(labelW + 8f, msg1Y + (msgZoneH - hint1.size.height) / 2f))
                        val hint2 = textMeasurer.measure("Material Flow ↓",
                            TextStyle(color = Color(0xFFBDC0C8), fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp))
                        drawText(hint2, topLeft = Offset(labelW + 8f, msg2Y + (msgZoneH - hint2.size.height) / 2f))
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
                OutlinedButton(onClick = onPrevStep, shape = RoundedCornerShape(6.dp), enabled = canPrev) {
                    Text("< Previous Step", color = if (canPrev) OnSurfaceVariant else OutlineVariant)
                }
                val iterLabel = when (state.currentStep) {
                    4 -> "Iteration: 4 (Global Sync Complete)"
                    else -> "Iteration: ${state.currentStep}"
                }
                Text(iterLabel, color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                val canNext = state.currentStep < 4 && state.amsSchedule.isNotEmpty() && !isRunning
                OutlinedButton(onClick = onNextStep, shape = RoundedCornerShape(6.dp), enabled = canNext) {
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
    topColor: Color, bottomColor: Color, step: Int,
    isCbmRow: Boolean = false,
    originalSchedule: List<TaskBlock> = emptyList()
) = with(scope) {
    // Row background
    drawRoundRect(Color(0xFFF1F5F9).copy(alpha = 0.5f),
        topLeft = Offset(labelW, laneY), size = Size(chartW, laneH),
        cornerRadius = CornerRadius(8.dp.toPx()))

    // Label background
    val labelBg = when (status) {
        MachineStatus.ANOMALY, MachineStatus.CONFLICT -> Color(0xFFFEE2E2)
        MachineStatus.RESOLVED, MachineStatus.DONE    -> Color(0xFFDCFCE7)
        MachineStatus.SHIFTED                          -> Color(0xFFEDE9FE)
        else                                           -> Color(0xFFE0E3E5)
    }
    val labelText = when (status) {
        MachineStatus.STEADY       -> "Steady State"
        MachineStatus.ANOMALY      -> "⚠ Anomaly Detected"
        MachineStatus.RESCHEDULING -> "⟳ Rescheduling…"
        MachineStatus.CONFLICT     -> "⚡ Conflict"
        MachineStatus.RESOLVED     -> "✓ Resolved"
        MachineStatus.SHIFTED      -> "↔ Shifted Schedule"
        MachineStatus.DONE         -> "✓ Sync Complete"
    }

    drawRoundRect(labelBg, topLeft = Offset(0f, laneY + 4.dp.toPx()),
        size = Size(labelW - 4.dp.toPx(), laneH - 8.dp.toPx()),
        cornerRadius = CornerRadius(8.dp.toPx()))

    val idLayout = measurer.measure(machineId,
        TextStyle(color = OnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold))
    drawText(idLayout, topLeft = Offset(8.dp.toPx(), laneY + 8.dp.toPx()))

    val stLayout = measurer.measure(labelText,
        TextStyle(color = when (status) {
            MachineStatus.ANOMALY, MachineStatus.CONFLICT -> Color(0xFFB91C1C)
            MachineStatus.RESOLVED, MachineStatus.DONE    -> Color(0xFF15803D)
            MachineStatus.SHIFTED                          -> Color(0xFF6D28D9)
            else                                           -> OnSurfaceVariant
        }, fontSize = 9.sp, fontWeight = FontWeight.SemiBold))
    drawText(stLayout, topLeft = Offset(8.dp.toPx(), laneY + 8.dp.toPx() + idLayout.size.height + 4.dp.toPx()))

    // Ghost original blocks (for shifted AMV)
    originalSchedule.forEach { block ->
        val sx = labelW + (block.startTime * scale).toFloat()
        val bw = (block.duration * scale).toFloat().coerceAtLeast(2f)
        val top = laneY + 12.dp.toPx(); val bh = laneH - 24.dp.toPx()
        drawRoundRect(Color(0xFFE5E7EB), topLeft = Offset(sx, top), size = Size(bw, bh),
            cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))
        val ol = measurer.measure("orig", TextStyle(color = Color(0xFFBFC3CC), fontSize = 9.sp))
        if (bw > ol.size.width + 4) drawText(ol, topLeft = Offset(sx + (bw - ol.size.width) / 2f, top + (bh - ol.size.height) / 2f))
    }

    // Actual blocks
    blocks.forEach { block ->
        val sx  = labelW + (block.startTime * scale).toFloat()
        val bw  = (block.duration * scale).toFloat().coerceAtLeast(2f)
        val top = laneY + 10.dp.toPx(); val bh = laneH - 20.dp.toPx()

        val (tc, bc, border, textCol) = when (block.type) {
            TaskType.CBM        -> listOf(Color(0xFFFECDD3), Color(0xFFFDA4AF), Color(0xFFFB7185), Color(0xFF881337))
            TaskType.TBM        -> listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF334155))
            TaskType.PRODUCTION -> listOf(topColor.copy(alpha = 0.6f), bottomColor.copy(alpha = 0.9f), bottomColor, Color(0xFF1E1B4B))
        }
        // Shadow
        drawRoundRect(Color.Black.copy(alpha = 0.08f), topLeft = Offset(sx, top + 3.dp.toPx()),
            size = Size(bw, bh), cornerRadius = CornerRadius(4.dp.toPx()))
        // Fill
        drawRoundRect(Brush.verticalGradient(listOf(tc, bc), startY = top, endY = top + bh),
            topLeft = Offset(sx, top), size = Size(bw, bh), cornerRadius = CornerRadius(4.dp.toPx()))
        // Border
        drawRoundRect(border, topLeft = Offset(sx, top), size = Size(bw, bh),
            cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))
        // Label
        val bl = measurer.measure(block.id, TextStyle(color = textCol, fontSize = 9.sp, fontWeight = FontWeight.Bold))
        if (bw > bl.size.width + 4)
            drawText(bl, topLeft = Offset(sx + (bw - bl.size.width) / 2f, top + (bh - bl.size.height) / 2f))
    }
}

private data class BlockColors(val top: Color, val bottom: Color, val border: Color, val text: Color)
private operator fun List<Color>.component4() = this[3]

private fun drawMessageArrow(
    scope: DrawScope, measurer: androidx.compose.ui.text.TextMeasurer,
    x: Float, zoneY: Float, zoneH: Float,
    msg: NegotiationMessage,
    arrowColor: Color, bgColor: Color, textColor: Color
) = with(scope) {
    val midY    = zoneY + zoneH / 2f
    val topY    = zoneY + 6.dp.toPx()
    val botY    = zoneY + zoneH - 6.dp.toPx()
    val tipSize = 6.dp.toPx()

    // Dashed vertical line
    drawLine(arrowColor, Offset(x, topY), Offset(x, botY - tipSize), 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)), cap = StrokeCap.Round)
    // Arrowhead
    val path = Path().apply {
        moveTo(x, botY); lineTo(x - tipSize, botY - tipSize); lineTo(x + tipSize, botY - tipSize); close()
    }
    drawPath(path, arrowColor)

    // Pill badge at midpoint
    val line1 = "${msg.type}: ${msg.jobId}"
    val line2 = "t${msg.originalTime.toInt()} → t${msg.requestedTime.toInt()} ${if (msg.accepted) "✓" else "✗"}"
    val l1    = measurer.measure(line1, TextStyle(color = textColor, fontSize = 8.sp, fontWeight = FontWeight.Bold))
    val l2    = measurer.measure(line2, TextStyle(color = textColor, fontSize = 8.sp))
    val pillW = maxOf(l1.size.width, l2.size.width) + 14f
    val pillH = l1.size.height + l2.size.height + 10f
    val pillX = (x - pillW / 2f).coerceAtLeast(0f)
    val pillY = midY - pillH / 2f

    drawRoundRect(bgColor, topLeft = Offset(pillX, pillY), size = Size(pillW, pillH),
        cornerRadius = CornerRadius(6.dp.toPx()))
    drawRoundRect(arrowColor, topLeft = Offset(pillX, pillY), size = Size(pillW, pillH),
        cornerRadius = CornerRadius(6.dp.toPx()), style = Stroke(1.dp.toPx()))
    drawText(l1, topLeft = Offset(pillX + 7f, pillY + 5f))
    drawText(l2, topLeft = Offset(pillX + 7f, pillY + 5f + l1.size.height + 2f))
}