package com.example.smartfactorymas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfactorymas.*
import kotlinx.coroutines.launch

// ─── Colour palette ───────────────────────────────────────────────────────────
object AppColors {
    val Primary    = Color(0xFF2962FF)
    val Secondary  = Color(0xFF00C853)
    val Background = Color(0xFFF8F9FA)
    val Surface    = Color.White
    val Outline    = Color(0xFFE0E0E0)
    val TextMain   = Color(0xFF212121)
    val TextMuted  = Color(0xFF757575)
    val Error      = Color(0xFFD50000)
    val Warning    = Color(0xFFFFAB00)
}

// ═══════════════════════════════════════════════════════════════════════════
//  Root composable
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun MaintenanceSchedulerRoot() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        AdvancedAnalyticsLabScreen()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Main screen
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AdvancedAnalyticsLabScreen() {
    val domain         = remember { SimulationDomain() }
    val scope          = rememberCoroutineScope()

    var strategyIsSOM  by remember { mutableStateOf(true) }
    var factorySize    by remember { mutableStateOf(20f) }
    var selectedTechs  by remember { mutableStateOf(4) }
    var isSimulating   by remember { mutableStateOf(false) }
    var batchResult    by remember { mutableStateOf<BatchResultEvent?>(null) }

    var table46Data    by remember { mutableStateOf(getEmptyTable46Data()) }
    var table47Data    by remember { mutableStateOf(getEmptyTable47Data()) }

    // Live node map
    var anomalyNodeId  by remember { mutableStateOf(4) }
    val affectedNodes  = listOf(anomalyNodeId + 1, anomalyNodeId + 2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Page title ────────────────────────────────────────────────────
        Text("Advanced Analytics Lab",
            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.TextMain)
        Text("System-wide operational simulation and anomaly tracking.",
            fontSize = 14.sp, color = AppColors.TextMuted)
        Spacer(modifier = Modifier.height(24.dp))

        // ── Top row: left panels (map + timeline) + right panels ──────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left 40 %
            Column(
                modifier = Modifier.weight(0.40f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FactoryNodeMap(anomalyNodeId, affectedNodes)
                FocusedTimelineView()
            }

            // Right 60 %
            Column(
                modifier = Modifier.weight(0.60f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ScenarioConfigCard(
                    factorySize      = factorySize,
                    onSizeChange     = { factorySize = it },
                    selectedTechs    = selectedTechs,
                    onTechsChange    = { selectedTechs = it },
                    strategyIsSOM    = strategyIsSOM,
                    onStrategyChange = { strategyIsSOM = it },
                    isSimulating     = isSimulating,
                    onRunClick = {
                        scope.launch {
                            isSimulating = true
                            table46Data  = getEmptyTable46Data()
                            table47Data  = getEmptyTable47Data()

                            val w1 = if (strategyIsSOM) 0.3 else 0.7
                            val w2 = 1.0 - w1

                            // Dashboard chart: quick batch for the selected config
                            batchResult = domain.runBatchSimulationCLI(
                                machines  = factorySize.toInt(),
                                jobs      = factorySize.toInt() * 3,
                                arhs      = selectedTechs,
                                w1 = w1, w2 = w2,
                                scenarios = 100
                            )

                            // ── Generate Tableau 4.6 ──────────────────────
                            // Each (m0, m1, m4) runs TWO batches: SOM and SOP.
                            // This is where SOM ≠ SOP timing comes from.
                            val new46 = mutableListOf<Table46Row>()
                            val configs = listOf(
                                Triple(5,  20,  2), Triple(5,  20,  4), Triple(5,  20,  8),
                                Triple(5,  50,  2), Triple(5,  50,  4), Triple(5,  50,  8),
                                Triple(5, 100,  2), Triple(5, 100,  4), Triple(5, 100,  8),
                                Triple(10, 20,  2), Triple(10, 20,  4), Triple(10, 20,  8),
                                Triple(10, 50,  2), Triple(10, 50,  4), Triple(10, 50,  8),
                                Triple(10,100,  2), Triple(10,100,  4), Triple(10,100,  8),
                                Triple(20, 20,  2), Triple(20, 20,  4), Triple(20, 20,  8),
                                Triple(20, 50,  2), Triple(20, 50,  4), Triple(20, 50,  8),
                                Triple(20,100,  2), Triple(20,100,  4), Triple(20,100,  8)
                            )

                            for ((m0, m1, m4) in configs) {
                                val som = domain.runBatchSimulationCLI(m0, m1, m4, 0.3, 0.7, 100)
                                val sop = domain.runBatchSimulationCLI(m0, m1, m4, 0.7, 0.3, 100)
                                new46.add(buildTable46Row(m0, m1, m4, som, sop))
                            }
                            table46Data = new46

                            // ── Generate Tableau 4.7 ──────────────────────
                            // Standardised on (m0=20, m1=60) to get stable
                            // averages; vary only m4.
                            val new47 = mutableListOf<Table47Row>()
                            for (m4 in listOf(2, 4, 8)) {
                                val som = domain.runBatchSimulationCLI(20, 60, m4, 0.3, 0.7, 100)
                                val sop = domain.runBatchSimulationCLI(20, 60, m4, 0.7, 0.3, 100)
                                new47.add(buildTable47Row(m4, som, sop))
                            }
                            table47Data = new47

                            isSimulating = false
                        }
                    }
                )

                StabilityReactivityPanel(batchResult, isSimulating)
                StrategicInsightCard(batchResult, isSimulating)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Academic tables ───────────────────────────────────────────────
        Text("Academic Data Visualization",
            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.TextMain)
        Text("Real-time benchmark generation matching the thesis methodology (Tableaux 4.6 & 4.7).",
            fontSize = 13.sp, color = AppColors.TextMuted)

        Spacer(modifier = Modifier.height(16.dp))
        FullTableau46(rows = table46Data)

        Spacer(modifier = Modifier.height(24.dp))
        FullTableau47(rows = table47Data)

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ─── Factory Node Map ─────────────────────────────────────────────────────────
@Composable
fun FactoryNodeMap(anomalyNodeId: Int, affectedNodes: List<Int>) {
    Card(
        colors       = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border       = BorderStroke(1.dp, AppColors.Outline),
        modifier     = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Factory Node Map", fontWeight = FontWeight.Bold)
                NodeBadge("LIVE", AppColors.Secondary)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                NodeCircle("3",  AppColors.TextMuted)
                Text(" → ", color = AppColors.TextMuted)
                NodeCircle(anomalyNodeId.toString(), AppColors.Error, filled = true)
                Text(" ⇢ ", color = AppColors.Warning, fontWeight = FontWeight.Bold)
                NodeCircle(affectedNodes[0].toString(), AppColors.Warning)
                Text(" ⇢ ", color = AppColors.Warning, fontWeight = FontWeight.Bold)
                NodeCircle(affectedNodes[1].toString(), AppColors.Warning)
                Text(" → ", color = AppColors.TextMuted)
                NodeCircle("…",  AppColors.TextMuted)
            }
            Spacer(modifier = Modifier.weight(1f))
            Divider(color = AppColors.Outline)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot("Anomaly",  AppColors.Error,   filled = true)
                LegendDot("Affected", AppColors.Warning, filled = false)
            }
        }
    }
}

@Composable
private fun NodeCircle(label: String, color: Color, filled: Boolean = false) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(if (filled) color else Color.Transparent)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (filled) Color.White else color,
            fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun LegendDot(label: String, color: Color, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape)
            .background(if (filled) color else Color.Transparent)
            .border(1.dp, color, CircleShape))
        Text(label, fontSize = 11.sp, color = AppColors.TextMuted)
    }
}

@Composable
private fun NodeBadge(text: String, color: Color) {
    Box(modifier = Modifier.border(1.dp, color, RoundedCornerShape(12.dp))
        .padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Focused Timeline View ────────────────────────────────────────────────────
@Composable
fun FocusedTimelineView() {
    Card(
        colors   = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border   = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth().height(220.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text("Focused Timeline View", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val lH = size.height / 4f
                val bH = lH * 0.6f
                // M3
                drawRoundRect(color = AppColors.Primary.copy(.2f), topLeft = Offset(0f, 0f),
                    size = Size(size.width * .4f, bH), cornerRadius = CornerRadius(4.dp.toPx()))
                // M4 anomaly
                drawRect(color = AppColors.Error.copy(.05f), topLeft = Offset(0f, lH),
                    size = Size(size.width, lH))
                drawRoundRect(color = AppColors.Error, topLeft = Offset(size.width * .3f, lH + 4.dp.toPx()),
                    size = Size(size.width * .2f, bH), cornerRadius = CornerRadius(4.dp.toPx()))
                drawLine(AppColors.Error, Offset(size.width * .4f, lH + bH),
                    Offset(size.width * .4f, lH * 2f), 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                // M5
                drawRoundRect(color = AppColors.Warning.copy(.5f),
                    topLeft = Offset(size.width * .4f, lH * 2f),
                    size = Size(size.width * .3f, bH), cornerRadius = CornerRadius(4.dp.toPx()))
                // M6
                drawRoundRect(color = AppColors.Primary.copy(.2f),
                    topLeft = Offset(size.width * .7f, lH * 3f),
                    size = Size(size.width * .3f, bH), cornerRadius = CornerRadius(4.dp.toPx()))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("08:00", fontSize = 10.sp, color = AppColors.TextMuted)
                Text("View: T-1h to T+4h", fontSize = 10.sp, color = AppColors.TextMuted,
                    fontWeight = FontWeight.Bold)
                Text("13:00", fontSize = 10.sp, color = AppColors.TextMuted)
            }
        }
    }
}

// ─── Scenario Config Card ─────────────────────────────────────────────────────
@Composable
fun ScenarioConfigCard(
    factorySize: Float, onSizeChange: (Float) -> Unit,
    selectedTechs: Int, onTechsChange: (Int) -> Unit,
    strategyIsSOM: Boolean, onStrategyChange: (Boolean) -> Unit,
    isSimulating: Boolean, onRunClick: () -> Unit
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border   = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Scenario Configurator", fontWeight = FontWeight.Bold)
                Button(onClick = onRunClick, enabled = !isSimulating,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                    shape  = RoundedCornerShape(8.dp)) {
                    Text(if (isSimulating) "⏳ Simulating…" else "⚗ Run Benchmark",
                        fontSize = 12.sp)
                }
            }

            // Factory size
            Column {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("FACTORY SIZE", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = AppColors.TextMuted)
                    Text("${factorySize.toInt()} Nodes", fontSize = 12.sp, color = AppColors.Primary)
                }
                Slider(value = factorySize, onValueChange = onSizeChange, valueRange = 5f..20f)
            }

            // Strategy toggle
            Row(modifier = Modifier.fillMaxWidth()
                .background(AppColors.Background, RoundedCornerShape(8.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("STRATEGY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppColors.TextMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SOP", fontSize = 12.sp,
                        color = if (!strategyIsSOM) AppColors.TextMain else AppColors.TextMuted)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = strategyIsSOM, onCheckedChange = onStrategyChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor  = AppColors.Primary,
                            checkedTrackColor  = AppColors.Primary.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(8.dp))
                    Text("SOM", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (strategyIsSOM) AppColors.TextMain else AppColors.TextMuted)
                }
            }

            // Technician segmented control
            Column {
                Text("MAINTENANCE ROSTER", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = AppColors.TextMuted)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()
                    .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))) {
                    listOf(2, 4, 8).forEach { t ->
                        val sel = selectedTechs == t
                        Box(modifier = Modifier.weight(1f)
                            .background(if (sel) AppColors.Primary.copy(.10f) else Color.Transparent)
                            .clickable { onTechsChange(t) }.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center) {
                            Text("$t Techs", fontSize = 12.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) AppColors.Primary else AppColors.TextMuted)
                        }
                    }
                }
            }
        }
    }
}

// ─── Stability + Reactivity Panel ────────────────────────────────────────────
@Composable
fun StabilityReactivityPanel(batchResult: BatchResultEvent?, isSimulating: Boolean) {
    val stablePct      = batchResult?.stability?.multi?.stable?.toFloat()   ?: 0f
    val improvedPct    = batchResult?.stability?.multi?.improved?.toFloat()  ?: 0f
    val detPct         = batchResult?.stability?.multi?.deteriorated?.toFloat() ?: 0f
    val totalPositive  = (stablePct + improvedPct).toInt()

    val stableAngle    = stablePct   / 100f * 360f
    val improvedAngle  = improvedPct / 100f * 360f
    val detAngle       = detPct      / 100f * 360f

    val avgSingle  = batchResult?.reactivity?.map { it.singleMs }?.average() ?: 0.0
    val avgMulti   = batchResult?.reactivity?.map { it.multiMs  }?.average() ?: 0.0
    val maxReact   = maxOf(avgSingle.toFloat(), avgMulti.toFloat(), 1f)

    Card(
        colors   = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border   = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            // Donut chart
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Global Factory Stability (Multi-Machine)",
                    fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                if (isSimulating) {
                    Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                } else {
                    Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val st = Stroke(22f, cap = StrokeCap.Butt)
                            drawArc(AppColors.Primary, -90f, stableAngle,   false, style = st)
                            drawArc(AppColors.Warning, -90f + stableAngle, improvedAngle, false, style = st)
                            drawArc(AppColors.Error,   -90f + stableAngle + improvedAngle, detAngle, false, style = st)
                        }
                        Text(if (batchResult == null) "--%"  else "$totalPositive%",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot("Stable",     AppColors.Primary, true)
                    LegendDot("Amélioré",   AppColors.Warning, true)
                    LegendDot("Détérioré",  AppColors.Error,   true)
                }
            }

            // Reactivity bars
            Column(modifier = Modifier.weight(1f)) {
                Text("System Reactivity (avg ms)", fontWeight = FontWeight.Bold,
                    color = AppColors.TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(20.dp))

                Text("1 Node (Local)", fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress    = if (batchResult == null) 0f else avgSingle.toFloat() / maxReact,
                        color       = AppColors.Primary,
                        trackColor  = AppColors.Background,
                        modifier    = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(if (batchResult == null) "--" else "%.2fms".format(avgSingle), fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text("Multi-Node (Global)", fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress    = if (batchResult == null) 0f else avgMulti.toFloat() / maxReact,
                        color       = AppColors.Warning,
                        trackColor  = AppColors.Background,
                        modifier    = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(if (batchResult == null) "--" else "%.2fms".format(avgMulti), fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = when {
                        batchResult == null -> "Run simulation to calculate ripple effect."
                        avgMulti < avgSingle -> "Multi-machine resolution uses localized heuristics — faster."
                        else -> "Multi-machine ripple effect adds avg +%.2fms.".format(avgMulti - avgSingle)
                    },
                    fontSize = 11.sp, color = AppColors.TextMuted, lineHeight = 16.sp
                )
            }
        }
    }
}

// ─── Strategic Insight Card ───────────────────────────────────────────────────
@Composable
fun StrategicInsightCard(batchResult: BatchResultEvent?, isSimulating: Boolean) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border   = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight()
                .background(AppColors.Primary))
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = AppColors.Primary,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Strategic AI Insight", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                when {
                    isSimulating  -> Text("Analyzing operations…", fontSize = 13.sp, color = AppColors.TextMuted)
                    batchResult == null ->
                        Text("Configure a scenario and click 'Run Benchmark' to generate insights.",
                            fontSize = 13.sp, color = AppColors.TextMuted)
                    else -> {
                        Text(batchResult.recommendation.replace("**",""),
                            fontSize = 13.sp, color = AppColors.TextMain, lineHeight = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        val (pillText, pillColor) = when {
                            batchResult.recommendation.contains("Diminishing") ->
                                "DIMINISHING RETURNS" to AppColors.Warning
                            batchResult.recommendation.contains("bottlenecked") ->
                                "BOTTLENECK WARNING" to AppColors.Error
                            else -> "OPTIMAL CONFIGURATION" to AppColors.Secondary
                        }
                        Box(modifier = Modifier
                            .background(pillColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("STATUS → $pillText", fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, color = pillColor)
                        }
                    }
                }
            }
        }
    }
}