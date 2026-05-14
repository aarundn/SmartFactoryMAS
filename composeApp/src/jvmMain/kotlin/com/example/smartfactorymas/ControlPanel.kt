package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ControlPanel(
    strategy: String, onStrategyChange: (String) -> Unit, w1: Float, onW1Change: (Float) -> Unit,
    anomalyTime: Double, onAnomalyTimeChange: (Double) -> Unit, schedulingStart: Double, onSchedulingStartChange: (Double) -> Unit,
    rulMin: Double, onRulMinChange: (Double) -> Unit, rulProb: Double, onRulProbChange: (Double) -> Unit, rulMax: Double, onRulMaxChange: (Double) -> Unit,
    jobs: List<JobInput>, onJobsChange: (List<JobInput>) -> Unit, tbms: List<TbmInput>, onTbmsChange: (List<TbmInput>) -> Unit, arhs: List<ArhUiState>, onArhsChange: (List<ArhUiState>) -> Unit,
    onSimulateClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(12.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

        Row(modifier = Modifier.fillMaxWidth().border(1.dp, SurfaceVariant).padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Factory Command Center", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        // Anomaly Simulator
        Column(modifier = Modifier.fillMaxWidth().background(SurfaceContainerLow, RoundedCornerShape(8.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(8.dp)).padding(16.dp)) {
            Text("ANOMALY SIMULATOR", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                SmallNumberField("Detection Time (t)", anomalyTime, Modifier.weight(1f)) { onAnomalyTimeChange(it) }
            }
            Button(
                onClick = onSimulateClick, modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PrimaryContainer, PrimaryFixed))), contentAlignment = Alignment.Center) {
                    Text("Simulate Anomaly", color = OnPrimaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Strategy Selection
        Column {
            Text("STRATEGY SELECTION", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(Modifier.fillMaxWidth().background(SurfaceContainerHighest, RoundedCornerShape(8.dp)).padding(4.dp)) {
                Segment("Strategy SOM", strategy == "SOM", { onStrategyChange("SOM"); onW1Change(0.75f) }, Modifier.weight(1f))
                Segment("Strategy SOP", strategy == "SOP", { onStrategyChange("SOP"); onW1Change(0.25f) }, Modifier.weight(1f))
            }
        }

        // Weights & General Setting
        Column {
            Text("KPI WEIGHTS & TIMING", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
            SmallNumberField("Scheduling Start Time", schedulingStart, Modifier.fillMaxWidth().padding(bottom = 12.dp)) { onSchedulingStartChange(it) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("w1", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(32.dp))

                @OptIn(ExperimentalMaterial3Api::class)
                Slider(
                    value = w1,
                    onValueChange = onW1Change,
                    modifier = Modifier.weight(1f),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Primary, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        // 🌟 Custom Track: شريط متصل تماماً بدون أي تعارض 🌟
                        val fraction = sliderState.value
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(SurfaceVariant, RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .background(Primary, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                )

                Text(
                    text = "%.2f".format(w1),
                    color = OnSurface,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(40.dp).padding(start = 8.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("w2", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(32.dp))

                @OptIn(ExperimentalMaterial3Api::class)
                Slider(
                    value = 1f - w1,
                    onValueChange = { onW1Change(1f - it) },
                    modifier = Modifier.weight(1f),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Primary, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        // 🌟 Custom Track لـ w2 🌟
                        val fraction = sliderState.value
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(SurfaceVariant, RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .background(Primary, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                )

                Text(
                    text = "%.2f".format(1f - w1),
                    color = OnSurface,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(40.dp).padding(start = 8.dp)
                )
            }
        }

        // RUL Prediction
        SectionBox("RUL Prediction") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallNumberField("RUL Min", rulMin, Modifier.weight(1f)) { onRulMinChange(it) }
                SmallNumberField("Probable", rulProb, Modifier.weight(1f)) { onRulProbChange(it) }
                SmallNumberField("RUL Max", rulMax, Modifier.weight(1f)) { onRulMaxChange(it) }
            }
        }

        // Jobs
        SectionBox("Production Jobs", onAdd = { onJobsChange(jobs + JobInput("P${jobs.size + 1}", 10.0, 50.0)) }) {
            jobs.forEachIndexed { i, job ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(SurfaceBright, RoundedCornerShape(6.dp)).border(1.dp, SurfaceVariant, RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(job.id, color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        IconButton(onClick = { onJobsChange(jobs.filterIndexed { index, _ -> index != i }) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null, tint = Error, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallStringField("Job ID", job.id, Modifier.weight(1f)) { onJobsChange(jobs.toMutableList().apply { this[i] = job.copy(id = it) }) }
                        SmallNumberField("Duration", job.duration, Modifier.weight(1f)) { onJobsChange(jobs.toMutableList().apply { this[i] = job.copy(duration = it) }) }
                        SmallNumberField("Due Date", job.dueDate, Modifier.weight(1f)) { onJobsChange(jobs.toMutableList().apply { this[i] = job.copy(dueDate = it) }) }
                    }
                }
            }
        }

        // TBMs
        SectionBox("TBM Activities", onAdd = { onTbmsChange(tbms + TbmInput("M${tbms.size + 1}", 50.0, 60.0)) }) {
            tbms.forEachIndexed { i, tbm ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(SurfaceBright, RoundedCornerShape(6.dp)).border(1.dp, SurfaceVariant, RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tbm.id, color = OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        IconButton(onClick = { onTbmsChange(tbms.filterIndexed { index, _ -> index != i }) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null, tint = Error, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallStringField("TBM ID", tbm.id, Modifier.weight(1f)) { onTbmsChange(tbms.toMutableList().apply { this[i] = tbm.copy(id = it) }) }
                        SmallNumberField("Start Time", tbm.start, Modifier.weight(1f)) { onTbmsChange(tbms.toMutableList().apply { this[i] = tbm.copy(start = it) }) }
                        SmallNumberField("End Time", tbm.end, Modifier.weight(1f)) { onTbmsChange(tbms.toMutableList().apply { this[i] = tbm.copy(end = it) }) }
                    }
                }
            }
        }

        // ARHs
        SectionBox("Technicians (ARH)", onAdd = { onArhsChange(arhs + ArhUiState("ARH_${arhs.size + 1}", 0.0, 100.0, 5.0, 5.0, 5.0)) }) {
            arhs.forEachIndexed { i, arh ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(SurfaceBright, RoundedCornerShape(6.dp)).border(1.dp, SurfaceVariant, RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(arh.id, color = Secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        IconButton(onClick = { onArhsChange(arhs.filterIndexed { index, _ -> index != i }) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null, tint = Error, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SmallStringField("Tech ID", arh.id, Modifier.fillMaxWidth().padding(bottom = 8.dp)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(id = it) }) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallNumberField("Avail Start", arh.availStart, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(availStart = it) }) }
                        SmallNumberField("Avail End", arh.availEnd, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(availEnd = it) }) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallNumberField("Min Dur", arh.durMin, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(durMin = it) }) }
                        SmallNumberField("Expected", arh.durProb, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(durProb = it) }) }
                        SmallNumberField("Max Dur", arh.durMax, Modifier.weight(1f)) { onArhsChange(arhs.toMutableList().apply { this[i] = arh.copy(durMax = it) }) }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionBox(title: String, onAdd: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(SurfaceContainerLow, RoundedCornerShape(8.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (onAdd != null) IconButton(onClick = onAdd, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Add, null, tint = Primary) }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun Segment(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(if (isSelected) SurfaceContainerLowest else Color.Transparent).clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) OnSurface else OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================================================
// 🌟 New Inputs (Label above the field)
// ============================================================================

@Composable
private fun SmallNumberField(label: String, value: Double, modifier: Modifier = Modifier, onChange: (Double) -> Unit) {
    var textValue by remember(value) { mutableStateOf(if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

        BasicTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText
                newText.toDoubleOrNull()?.let { onChange(it) }
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = OnSurface, fontFamily = FontFamily.Monospace),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize().background(SurfaceContainerLowest, RoundedCornerShape(6.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textValue.isEmpty()) {
                        Text("0", color = OutlineVariant, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun SmallStringField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = OnSurface, fontFamily = FontFamily.Monospace),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(36.dp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize().background(SurfaceContainerLowest, RoundedCornerShape(6.dp)).border(1.dp, OutlineVariant, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text("...", color = OutlineVariant, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    innerTextField()
                }
            }
        )
    }
}