package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ControlPanel(
    selectedStrategy: String,
    onStrategyChange: (String) -> Unit,
    w1: Float,
    onW1Change: (Float) -> Unit,
    anomalyTime: Int,
    onAnomalyTimeChange: (Int) -> Unit,
    anomalySeverity: Float,
    onSeverityChange: (Float) -> Unit,
    cbmBaseDuration: Int,
    onCbmBaseChange: (Int) -> Unit,
    arhs: List<ArhUiState>,
    onArhsChange: (List<ArhUiState>) -> Unit,
    onSimulateClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Card 1: Anomaly Simulator
        DashboardCard {
            Text("Anomaly Simulator", color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))

            Text("Detection Time (t)", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = anomalyTime.toString(),
                onValueChange = { onAnomalyTimeChange(it.toIntOrNull() ?: 0) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OutlineVariant)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onSimulateClick,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("\uD83D\uDEA8 Simulate Anomaly", color = OnTertiary, fontWeight = FontWeight.Medium)
            }
        }

        // Card 2: Strategy Control
        DashboardCard {
            Text("Strategy Control", color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(SurfaceContainerHigh, RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Segment("SOM (Safety)", selectedStrategy == "SOM", { onStrategyChange("SOM") }, Modifier.weight(1f))
                Segment("SOP (Production)", selectedStrategy == "SOP", { onStrategyChange("SOP") }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // KPI Weights
            Text("KPI WEIGHTS", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("w1", Modifier.width(32.dp), color = OnSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Slider(value = w1, onValueChange = onW1Change, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary))
                Text("%.2f".format(w1), Modifier.width(40.dp), color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("w2", Modifier.width(32.dp), color = OnSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Slider(value = 1f - w1, onValueChange = { onW1Change(1f - it) }, modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary))
                Text("%.2f".format(1f - w1), Modifier.width(40.dp), color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Card 3: ARH Manager with add/remove
        DashboardCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ARH Manager", color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                TextButton(
                    onClick = {
                        val newId = "ARH_${arhs.size + 1}"
                        onArhsChange(arhs + ArhUiState(newId, 24.0, 50.0, 4.0, 7.0, 9.0))
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+ Add ARH", color = Primary)
                }
            }

            Spacer(Modifier.height(8.dp))

            arhs.forEachIndexed { idx, arh ->
                ArhCard(arh, idx, arhs.size,
                    onUpdate = { updated -> onArhsChange(arhs.toMutableList().also { it[idx] = updated }) },
                    onRemove = { onArhsChange(arhs.toMutableList().also { it.removeAt(idx) }) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ArhCard(arh: ArhUiState, idx: Int, total: Int, onUpdate: (ArhUiState) -> Unit, onRemove: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(SurfaceBright, RoundedCornerShape(8.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
            Spacer(Modifier.width(12.dp))
            Text(arh.id, fontWeight = FontWeight.Medium, color = OnSurface, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text("[${arh.availStart.toInt()}-${arh.availEnd.toInt()}]", color = OnSurfaceVariant, fontSize = 11.sp)
            if (total > 1) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, tint = Error, modifier = Modifier.size(14.dp))
                }
            }
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            // Editable fields
            SmallField("ID", arh.id) { onUpdate(arh.copy(id = it)) }
            SmallNumberField("Avail Start", arh.availStart) { onUpdate(arh.copy(availStart = it)) }
            SmallNumberField("Avail End", arh.availEnd) { onUpdate(arh.copy(availEnd = it)) }
            SmallNumberField("Dur Min", arh.durMin) { onUpdate(arh.copy(durMin = it)) }
            SmallNumberField("Dur Prob", arh.durProb) { onUpdate(arh.copy(durProb = it)) }
            SmallNumberField("Dur Max", arh.durMax) { onUpdate(arh.copy(durMax = it)) }
        }
    }
}

@Composable
private fun SmallField(label: String, value: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(70.dp), color = OnSurfaceVariant, fontSize = 11.sp)
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = OutlineVariant, focusedBorderColor = Primary)
        )
    }
}

@Composable
private fun SmallNumberField(label: String, value: Double, onChange: (Double) -> Unit) {
    SmallField(label, value.toString()) { onChange(it.toDoubleOrNull() ?: value) }
}

@Composable
private fun Segment(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) SurfaceContainerLowest else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(Modifier.matchParentSize().shadow(1.dp, RoundedCornerShape(6.dp)))
        }
        Text(text, color = if (isSelected) OnSurface else OnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
