package com.example.smartfactorymas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfactorymas.BatchResultEvent
import com.example.smartfactorymas.FullTableau46
import com.example.smartfactorymas.FullTableau47
import com.example.smartfactorymas.SimulationDomain
import com.example.smartfactorymas.Table46Row
import com.example.smartfactorymas.Table47Row
import com.example.smartfactorymas.buildTable46Row
import com.example.smartfactorymas.buildTable47Row
import com.example.smartfactorymas.getEmptyTable46Data
import com.example.smartfactorymas.getEmptyTable47Data
import com.example.smartfactorymas.keyboardAndCursorScroll
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

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .keyboardAndCursorScroll(scrollState, scope)
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

            // Right 60 %
            Column(
                modifier = Modifier.weight(0.60f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                    Button(onClick ={
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
                                scenarios = 25
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
                                val som = domain.runBatchSimulationCLI(m0, m1, m4, 0.3, 0.7, 25)
                                val sop = domain.runBatchSimulationCLI(m0, m1, m4, 0.7, 0.3, 25)
                                new46.add(buildTable46Row(m0, m1, m4, som, sop))
                            }
                            table46Data = new46

                            // ── Generate Tableau 4.7 ──────────────────────
                            // Uses Taillard benchmark group ta031-ta040
                            // (50 jobs) for deterministic results; vary m4.
                            val new47 = mutableListOf<Table47Row>()
                            for (m4 in listOf(2, 4, 8)) {
                                val som = domain.runBatchSimulationCLI(20, 50, m4, 0.3, 0.7, 25)
                                val sop = domain.runBatchSimulationCLI(20, 50, m4, 0.7, 0.3, 25)
                                new47.add(buildTable47Row(m4, som, sop))
                            }
                            table47Data = new47

                            isSimulating = false
                        }
                    }, enabled = !isSimulating,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                        shape  = RoundedCornerShape(8.dp)) {
                        Text(if (isSimulating) "⏳ Simulating…" else "⚗ Run Benchmark",
                            fontSize = 12.sp)
                    }


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