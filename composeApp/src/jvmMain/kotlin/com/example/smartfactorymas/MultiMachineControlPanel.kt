package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MultiMachineControlPanel(
    mmState: MultiMachineState,
    amaConfig: AmaUiConfig,
    amvConfig: AmvUiConfig,
    isRunning: Boolean,
    hasLocalResult: Boolean,
    onMmStateChange: (MultiMachineState) -> Unit,
    onAmaChange: (AmaUiConfig) -> Unit,
    onAmvChange: (AmvUiConfig) -> Unit,
    onSimulate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().border(1.dp, SurfaceVariant).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Global Factory Control", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        // ── Anomaly Simulator ──
        Column(modifier = Modifier.fillMaxWidth()
            .background(SurfaceContainerLow, RoundedCornerShape(8.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp)).padding(16.dp)) {
            Text("SIMULATION PARAMETERS", color = OnSurfaceVariant, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))

            MmNumberField("Anomaly Time (t)", mmState.anomalyTime, Modifier.fillMaxWidth().padding(bottom = 8.dp))
            { onMmStateChange(mmState.copy(anomalyTime = it)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MmNumberField("AMA Delivery Time", mmState.amaDeliveryTime, Modifier.weight(1f))
                { onMmStateChange(mmState.copy(amaDeliveryTime = it)) }
                MmNumberField("AMV Expected Time", mmState.amvExpectedTime, Modifier.weight(1f))
                { onMmStateChange(mmState.copy(amvExpectedTime = it)) }
            }
            Spacer(Modifier.height(12.dp))

            // Simulate button
            Button(onClick = onSimulate,
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(0.dp)) {
                Box(modifier = Modifier.fillMaxSize()
                    .background(if (!isRunning)
                        Brush.verticalGradient(listOf(PrimaryContainer, PrimaryFixed))
                    else Brush.verticalGradient(listOf(SurfaceVariant, SurfaceVariant))),
                    contentAlignment = Alignment.Center) {
                    Text(if (isRunning) "Running…" else "▶  Simulate Factory Anomaly",
                        color = if (!isRunning) OnPrimaryContainer else OnSurfaceVariant,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!hasLocalResult) {
                Spacer(Modifier.height(8.dp))
                Text("⚠ Run Local (AMS) simulation first to populate AMS schedule.",
                    color = Error, fontSize = 11.sp)
            }
        }

        // ── AMA Machine Config ──
        SectionBox("Upstream Machine (AMA)",
            onAdd = { onAmaChange(amaConfig.copy(blocks = amaConfig.blocks + NeighborBlockUi("PRD-AMA-${amaConfig.blocks.size + 1}", 0.0, 30.0))) }) {
            MmStringField("Machine ID", amaConfig.id, Modifier.fillMaxWidth().padding(bottom = 8.dp))
            { onAmaChange(amaConfig.copy(id = it)) }
            amaConfig.blocks.forEachIndexed { i, b ->
                ItemCard(b.jobId, { onAmaChange(amaConfig.copy(blocks = amaConfig.blocks.filterIndexed { idx, _ -> idx != i })) }) {
                    MmStringField("Job", b.jobId, Modifier.weight(1f)) {
                        onAmaChange(amaConfig.copy(blocks = amaConfig.blocks.toMutableList().apply { this[i] = b.copy(jobId = it) }))
                    }
                    MmNumberField("Start", b.start, Modifier.weight(1f)) {
                        onAmaChange(amaConfig.copy(blocks = amaConfig.blocks.toMutableList().apply { this[i] = b.copy(start = it) }))
                    }
                    MmNumberField("End", b.end, Modifier.weight(1f)) {
                        onAmaChange(amaConfig.copy(blocks = amaConfig.blocks.toMutableList().apply { this[i] = b.copy(end = it) }))
                    }
                }
            }
        }

        // ── AMV Machine Config ──
        SectionBox("Downstream Machine (AMV)",
            onAdd = { onAmvChange(amvConfig.copy(blocks = amvConfig.blocks + NeighborBlockUi("PRD-AMV-${amvConfig.blocks.size + 1}", 80.0, 120.0))) }) {
            MmStringField("Machine ID", amvConfig.id, Modifier.fillMaxWidth().padding(bottom = 8.dp))
            { onAmvChange(amvConfig.copy(id = it)) }
            amvConfig.blocks.forEachIndexed { i, b ->
                ItemCard(b.jobId, { onAmvChange(amvConfig.copy(blocks = amvConfig.blocks.filterIndexed { idx, _ -> idx != i })) }) {
                    MmStringField("Job", b.jobId, Modifier.weight(1f)) {
                        onAmvChange(amvConfig.copy(blocks = amvConfig.blocks.toMutableList().apply { this[i] = b.copy(jobId = it) }))
                    }
                    MmNumberField("Start", b.start, Modifier.weight(1f)) {
                        onAmvChange(amvConfig.copy(blocks = amvConfig.blocks.toMutableList().apply { this[i] = b.copy(start = it) }))
                    }
                    MmNumberField("End", b.end, Modifier.weight(1f)) {
                        onAmvChange(amvConfig.copy(blocks = amvConfig.blocks.toMutableList().apply { this[i] = b.copy(end = it) }))
                    }
                }
            }
        }
    }
}

@Composable
private fun MmNumberField(label: String, value: Double, modifier: Modifier = Modifier, onChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        BasicTextField(value = text, onValueChange = { text = it; it.toDoubleOrNull()?.let(onChange) },
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = OnSurface, fontFamily = FontFamily.Monospace),
            singleLine = true, modifier = Modifier.fillMaxWidth().height(36.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize().background(SurfaceContainerLowest, RoundedCornerShape(6.dp))
                    .border(1.dp, OutlineVariant, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) Text("0", color = OutlineVariant, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    inner()
                }
            })
    }
}

@Composable
private fun MmStringField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        BasicTextField(value = value, onValueChange = onChange,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = OnSurface, fontFamily = FontFamily.Monospace),
            singleLine = true, modifier = Modifier.fillMaxWidth().height(36.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize().background(SurfaceContainerLowest, RoundedCornerShape(6.dp))
                    .border(1.dp, OutlineVariant, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text("…", color = OutlineVariant, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    inner()
                }
            })
    }
}

@Composable
fun ItemCard(
    title: String,
    onDelete: () -> Unit,
    // نستخدم RowScope لأن العناصر بداخلها (الحقول) تستخدم Modifier.weight(1f)
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(SurfaceBright, RoundedCornerShape(6.dp))
            .border(1.dp, SurfaceVariant, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        // ── Header (Title & Close Button) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.ifEmpty { "..." },
                color = Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Content (The input fields) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}