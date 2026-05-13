package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ControlPanel(
    strategy: String, onStrategyChange: (String) -> Unit,
    w1: Float, onW1Change: (Float) -> Unit,
    anomalyTime: Double, onAnomalyTimeChange: (Double) -> Unit,
    schedulingStart: Double, onSchedulingStartChange: (Double) -> Unit,
    rulMin: Double, onRulMinChange: (Double) -> Unit,
    rulProb: Double, onRulProbChange: (Double) -> Unit,
    rulMax: Double, onRulMaxChange: (Double) -> Unit,
    jobs: List<JobInput>, onJobsChange: (List<JobInput>) -> Unit,
    tbms: List<TbmInput>, onTbmsChange: (List<TbmInput>) -> Unit,
    arhs: List<ArhUiState>, onArhsChange: (List<ArhUiState>) -> Unit,
    onSimulateClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Text("Factory Command Center", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Button(onClick = onSimulateClick, modifier = Modifier.fillMaxWidth().height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Text("Run Simulation", fontWeight = FontWeight.Bold)
        }

        // Global Configuration
        SectionBox("Configuration & Strategy") {
            Row(Modifier.fillMaxWidth()) {
                Segment("SOM (Maintenance)", strategy == "SOM", { onStrategyChange("SOM") }, Modifier.weight(1f))
                Segment("SOP (Production)", strategy == "SOP", { onStrategyChange("SOP") }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            SmallNumberField("Start Time", schedulingStart) { onSchedulingStartChange(it) }
            SmallNumberField("Weight w1", w1.toDouble()) { onW1Change(it.toFloat()) }
        }

        // Anomaly Details
        SectionBox("Anomaly & RUL Prediction") {
            SmallNumberField("Alert Time (t)", anomalyTime) { onAnomalyTimeChange(it) }
            SmallNumberField("RUL Min", rulMin) { onRulMinChange(it) }
            SmallNumberField("RUL Probable", rulProb) { onRulProbChange(it) }
            SmallNumberField("RUL Max", rulMax) { onRulMaxChange(it) }
        }

        // Jobs
        SectionBox("Production Jobs (Flexible)", onAdd = { onJobsChange(jobs + JobInput("P${jobs.size + 1}", 10.0, 50.0)) }) {
            jobs.forEachIndexed { i, job ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(SurfaceContainerLow, RoundedCornerShape(4.dp)).padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SmallStringField("Job Name", job.id, Modifier.weight(1f)) { onJobsChange(jobs.toMutableList().apply { this[i] = job.copy(id = it) }) }
                        IconButton(onClick = { onJobsChange(jobs.filterIndexed { index, _ -> index != i }) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Error)
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        SmallNumberField("Duration", job.duration, Modifier.weight(1f)) { onJobsChange(jobs.toMutableList().apply { this[i] = job.copy(duration = it) }) }
                        Spacer(Modifier.width(8.dp))
                        SmallNumberField("Due Date", job.dueDate, Modifier.weight(1f)) { onJobsChange(jobs.toMutableList().apply { this[i] = job.copy(dueDate = it) }) }
                    }
                }
            }
        }

        // TBMs
        SectionBox("TBM Activities (Fixed)", onAdd = { onTbmsChange(tbms + TbmInput("M${tbms.size + 1}", 50.0, 60.0)) }) {
            tbms.forEachIndexed { i, tbm ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(SurfaceContainerLow, RoundedCornerShape(4.dp)).padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SmallStringField("TBM Name", tbm.id, Modifier.weight(1f)) { onTbmsChange(tbms.toMutableList().apply { this[i] = tbm.copy(id = it) }) }
                        IconButton(onClick = { onTbmsChange(tbms.filterIndexed { index, _ -> index != i }) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Error)
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        SmallNumberField("Start Time", tbm.start, Modifier.weight(1f)) { onTbmsChange(tbms.toMutableList().apply { this[i] = tbm.copy(start = it) }) }
                        Spacer(Modifier.width(8.dp))
                        SmallNumberField("End Time", tbm.end, Modifier.weight(1f)) { onTbmsChange(tbms.toMutableList().apply { this[i] = tbm.copy(end = it) }) }
                    }
                }
            }
        }

        // ARHs
        SectionBox("Technicians (ARH)", onAdd = { onArhsChange(arhs + ArhUiState("ARH_${arhs.size + 1}", 0.0, 100.0, 5.0, 5.0, 5.0)) }) {
            arhs.forEachIndexed { i, arh ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(SurfaceContainerLow, RoundedCornerShape(4.dp)).padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SmallStringField("Tech Name", arh.id, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(id = it) }) }
                        IconButton(onClick = { onArhsChange(arhs.filterIndexed { index, _ -> index != i }) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Error)
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        SmallNumberField("Avail Start", arh.availStart, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(availStart = it) }) }
                        Spacer(Modifier.width(8.dp))
                        SmallNumberField("Avail End", arh.availEnd, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(availEnd = it) }) }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        SmallNumberField("Min Dur", arh.durMin, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(durMin = it) }) }
                        Spacer(Modifier.width(4.dp))
                        SmallNumberField("Expected", arh.durProb, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(durProb = it) }) }
                        Spacer(Modifier.width(4.dp))
                        SmallNumberField("Max Dur", arh.durMax, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(durMax = it) }) }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionBox(title: String, onAdd: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(SurfaceContainerHigh, RoundedCornerShape(8.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (onAdd != null) IconButton(onClick = onAdd, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Add, null, tint = Primary) }
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SmallNumberField(label: String, value: Double, modifier: Modifier = Modifier, onChange: (Double) -> Unit) {
    // Local string state to allow smooth typing of decimals
    var textValue by remember(value) { mutableStateOf(if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()) }

    Row(modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(65.dp), color = OnSurfaceVariant, fontSize = 10.sp, maxLines = 1)

        BasicTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText
                newText.toDoubleOrNull()?.let { onChange(it) }
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = OnSurface),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(32.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize().background(SurfaceBright, RoundedCornerShape(4.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun SmallStringField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    Row(modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(65.dp), color = OnSurfaceVariant, fontSize = 10.sp, maxLines = 1)

        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = OnSurface),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(32.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize().background(SurfaceBright, RoundedCornerShape(4.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun Segment(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(if (isSelected) Primary else Color.Transparent).clickable(onClick = onClick).padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) OnPrimary else OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}