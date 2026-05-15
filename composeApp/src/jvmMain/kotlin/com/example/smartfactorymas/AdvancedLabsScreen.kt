
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfactorymas.BatchResultEvent
import com.example.smartfactorymas.SimulationDomain
import kotlinx.coroutines.launch

// ============================================================
//  Theme / Color Palette Fallbacks
// ============================================================
object AppColors {
    val Primary = Color(0xFF2962FF)
    val Secondary = Color(0xFF00C853)
    val Background = Color(0xFFF8F9FA)
    val Surface = Color.White
    val Outline = Color(0xFFE0E0E0)
    val TextMain = Color(0xFF212121)
    val TextMuted = Color(0xFF757575)
    val Error = Color(0xFFD50000)
    val Warning = Color(0xFFFFAB00)
}

// ============================================================
//  1. Root Layout & Shell (TopBar + Sidebar)
// ============================================================
@Composable
fun MaintenanceSchedulerRoot() {
    Row(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

            // Main Content Area
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                AdvancedAnalyticsLabScreen()
            }
        }
    }
}

@Composable
fun Sidebar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Outline)
            .padding(16.dp)
    ) {
        Text("FLEET CONTROL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.TextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Active Ops", fontSize = 14.sp, color = AppColors.Secondary, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(24.dp))

        SidebarItem(Icons.Default.Dashboard, "Dashboard", isSelected = true)
        SidebarItem(Icons.Default.Timeline, "Timeline")
        SidebarItem(Icons.Default.Person, "Technicians")
        SidebarItem(Icons.Default.Build, "Assets")
        SidebarItem(Icons.Default.Settings, "Configuration")

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextMuted)
        ) {
            Text("+ New Maintenance Task", fontSize = 12.sp)
        }
    }
}

@Composable
fun SidebarItem(icon: ImageVector, label: String, isSelected: Boolean = false) {
    val bgColor = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (isSelected) AppColors.Primary else AppColors.TextMuted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { }
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = contentColor, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
    }
}




// ============================================================
//  2. Advanced Analytics Lab Screen (Main Grid)
// ============================================================
@Composable
fun AdvancedAnalyticsLabScreen() {
    // 🌟 1. Instantiate your Simulation Engine & Coroutine Scope
    val domain = remember { SimulationDomain() }
    val coroutineScope = rememberCoroutineScope()

    // 🌟 2. Shared State
    var strategyIsSOM by remember { mutableStateOf(true) }
    var anomalyNodeId by remember { mutableStateOf(4) }
    var factorySize by remember { mutableStateOf(20f) }
    var selectedTechs by remember { mutableStateOf(4) }

    // 🌟 3. Execution State
    var isSimulating by remember { mutableStateOf(false) }
    var batchResult by remember { mutableStateOf<BatchResultEvent?>(null) }

    // Derived
    val w1 = if (strategyIsSOM) 0.3 else 0.7
    val w2 = 1.0 - w1
    val affectedNodes = listOf(anomalyNodeId + 1, anomalyNodeId + 2)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Advanced Analytics Lab", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.TextMain)
        Text("System-wide operational simulation and anomaly tracking.", fontSize = 14.sp, color = AppColors.TextMuted)

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ScenarioConfigCard(
                    factorySize = factorySize,
                    onFactorySizeChange = { factorySize = it },
                    selectedTechs = selectedTechs,
                    onTechsChange = { selectedTechs = it },
                    strategyIsSOM = strategyIsSOM,
                    onStrategyChange = { strategyIsSOM = it },
                    isSimulating = isSimulating,
                    onRunClick = {
                        // 🌟 4. Execute the Simulation when clicked!
                        coroutineScope.launch {
                            isSimulating = true
                            batchResult = domain.runBatchSimulationCLI(
                                machines = factorySize.toInt(),
                                jobs = factorySize.toInt() * 3, // Arbitrary job scaling
                                arhs = selectedTechs,
                                w1 = w1,
                                w2 = w2,
                                scenarios = 100
                            )
                            isSimulating = false
                        }
                    }
                )

                // Pass the dynamic results down to the charts
                StabilityReactivityPanel(batchResult, isSimulating)
                StrategicInsightCard(batchResult, isSimulating)
            }
        }
    }
}

// ============================================================
//  🔴 NEW Component: Factory Node Map
// ============================================================
@Composable
fun FactoryNodeMap(anomalyNodeId: Int, affectedNodes: List<Int>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth().height(280.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Factory Node Map", fontWeight = FontWeight.Bold)
                Badge("LIVE VIEW", AppColors.Secondary)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Simplified Representation of the 2D Grid + Arrows
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                NodeCircle("3", AppColors.TextMuted, isDashed = true)
                Text(" → ", color = AppColors.TextMuted)

                // Anomaly
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(AppColors.Error.copy(alpha = 0.2f)))
                    NodeCircle(anomalyNodeId.toString(), AppColors.Error, filled = true)
                }

                Text(" ⇢ ", color = AppColors.Warning, fontWeight = FontWeight.Bold)

                // Affected
                NodeCircle(affectedNodes[0].toString(), AppColors.Warning, isDashed = true)
                Text(" ⇢ ", color = AppColors.Warning, fontWeight = FontWeight.Bold)
                NodeCircle(affectedNodes[1].toString(), AppColors.Warning, isDashed = true)
            }

            Spacer(modifier = Modifier.weight(1f))
            Divider(color = AppColors.Outline)
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("Anomaly", AppColors.Error, filled = true)
                LegendItem("Affected", AppColors.Warning, filled = false)
            }
        }
    }
}

@Composable
fun NodeCircle(text: String, color: Color, filled: Boolean = false, isDashed: Boolean = false) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (filled) color else Color.Transparent)
            .border(
                width = 2.dp,
                color = color,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (filled) Color.White else color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LegendItem(label: String, color: Color, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (filled) color else Color.Transparent).border(1.dp, color, CircleShape))
        Text(label, fontSize = 12.sp, color = AppColors.TextMuted)
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Box(modifier = Modifier.border(1.dp, color, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ============================================================
//  🔴 NEW Component: Focused Timeline View
// ============================================================
@Composable
fun FocusedTimelineView() {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth().height(260.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Text("Focused Timeline View", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val laneHeight = size.height / 4
                val blockHeight = laneHeight * 0.6f

                // M3 (Standard)
                drawRoundRect(color = AppColors.Primary.copy(alpha = 0.2f), topLeft = Offset(0f, 0f), size = Size(size.width * 0.4f, blockHeight), cornerRadius = CornerRadius(4.dp.toPx()))

                // M4 (Anomaly Lane) - Red Tint
                drawRect(color = AppColors.Error.copy(alpha = 0.05f), topLeft = Offset(0f, laneHeight), size = Size(size.width, laneHeight))
                drawRoundRect(color = AppColors.Error, topLeft = Offset(size.width * 0.3f, laneHeight + 5.dp.toPx()), size = Size(size.width * 0.2f, blockHeight), cornerRadius = CornerRadius(4.dp.toPx()))

                // Connection Line
                drawLine(color = AppColors.Error, start = Offset(size.width * 0.4f, laneHeight + blockHeight), end = Offset(size.width * 0.4f, laneHeight * 2), strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))

                // M5 (Affected)
                drawRoundRect(color = AppColors.Warning.copy(alpha = 0.5f), topLeft = Offset(size.width * 0.4f, laneHeight * 2), size = Size(size.width * 0.3f, blockHeight), cornerRadius = CornerRadius(4.dp.toPx()))

                // M6 (Standard)
                drawRoundRect(color = AppColors.Primary.copy(alpha = 0.2f), topLeft = Offset(size.width * 0.7f, laneHeight * 3), size = Size(size.width * 0.3f, blockHeight), cornerRadius = CornerRadius(4.dp.toPx()))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("08:00", fontSize = 10.sp, color = AppColors.TextMuted)
                Text("View: T-1h to T+4h", fontSize = 10.sp, color = AppColors.TextMuted, fontWeight = FontWeight.Bold)
                Text("13:00", fontSize = 10.sp, color = AppColors.TextMuted)
            }
        }
    }
}

// ============================================================
//  🔴 NEW Component: Scenario Config Card
// ============================================================
@Composable
fun ScenarioConfigCard(
    factorySize: Float, onFactorySizeChange: (Float) -> Unit,
    selectedTechs: Int, onTechsChange: (Int) -> Unit,
    strategyIsSOM: Boolean, onStrategyChange: (Boolean) -> Unit,
    isSimulating: Boolean, onRunClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Scenario Configurator", fontWeight = FontWeight.Bold)

                // Dynamic Run Button
                Button(
                    onClick = onRunClick,
                    enabled = !isSimulating,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isSimulating) "⏳ Simulating..." else "⚗ Run", fontSize = 12.sp)
                }
            }

            // Factory Size Slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("FACTORY SIZE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppColors.TextMuted)
                    Text("${factorySize.toInt()} Nodes", fontSize = 12.sp, color = AppColors.Primary)
                }
                Slider(value = factorySize, onValueChange = onFactorySizeChange, valueRange = 5f..20f)
            }

            // Strategy Switch
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().background(AppColors.Background, RoundedCornerShape(8.dp)).padding(12.dp)) {
                Text("MAINTENANCE STRATEGY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppColors.TextMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SOP", fontSize = 12.sp, color = if (!strategyIsSOM) AppColors.TextMain else AppColors.TextMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = strategyIsSOM, onCheckedChange = onStrategyChange, colors = SwitchDefaults.colors(checkedThumbColor = AppColors.Primary, checkedTrackColor = AppColors.Primary.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SOM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (strategyIsSOM) AppColors.TextMain else AppColors.TextMuted)
                }
            }

            // Tech Roster Segmented
            Column {
                Text("MAINTENANCE ROSTER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppColors.TextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
                    listOf(2, 4, 8).forEach { techs ->
                        val isSelected = selectedTechs == techs
                        Box(
                            modifier = Modifier.weight(1f).background(if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { onTechsChange(techs) }.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$techs Techs", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) AppColors.Primary else AppColors.TextMuted)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
//  🔴 NEW Component: Stability Reactivity Panel
// ============================================================
// ============================================================
//  🔴 Component: Stability Reactivity Panel (FIXED DECIMALS)
// ============================================================
@Composable
fun StabilityReactivityPanel(batchResult: BatchResultEvent?, isSimulating: Boolean) {

    // Extract Dynamic Data
    val stablePct = batchResult?.stability?.stable?.toFloat() ?: 0f
    val improvedPct = batchResult?.stability?.improved?.toFloat() ?: 0f
    val deterioratedPct = batchResult?.stability?.deteriorated?.toFloat() ?: 0f

    val totalPositive = (stablePct + improvedPct).toInt()

    // Convert percentages to 360-degree angles
    val stableAngle = (stablePct / 100f) * 360f
    val improvedAngle = (improvedPct / 100f) * 360f
    val deterioratedAngle = (deterioratedPct / 100f) * 360f

    // 🌟 FIX: Use double precision for the averages
    val avgSingle = batchResult?.reactivity?.map { it.singleMs }?.average() ?: 0.0
    val avgMulti = batchResult?.reactivity?.map { it.multiMs }?.average() ?: 0.0
    val maxReactivity = maxOf(avgSingle.toFloat(), avgMulti.toFloat(), 1f)

    // Helper function to format the number to 2 decimal places
    fun formatMs(value: Double): String {
        return "%.2f".format(value)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth()) {

            // ── Left: Donut Chart ──
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Factory Stability Index", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                if (isSimulating) {
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                } else {
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeStyle = Stroke(width = 20f, cap = StrokeCap.Butt)
                            drawArc(AppColors.Primary, -90f, stableAngle, false, style = strokeStyle)
                            drawArc(AppColors.Warning, -90f + stableAngle, improvedAngle, false, style = strokeStyle)
                            drawArc(AppColors.Error, -90f + stableAngle + improvedAngle, deterioratedAngle, false, style = strokeStyle)
                        }
                        Text(if (batchResult == null) "--%" else "${totalPositive}%", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem("Stable", AppColors.Primary, true)
                    LegendItem("Amélioré", AppColors.Warning, true)
                    LegendItem("Détérioré", AppColors.Error, true)
                }
            }

            // ── Right: Reactivity Bars ──
            Column(modifier = Modifier.weight(1f)) {
                Text("System Reactivity (Avg ms)", fontWeight = FontWeight.Bold, color = AppColors.TextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Text("1 Node (Local)", fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = if (isSimulating || batchResult == null) 0f else (avgSingle.toFloat() / maxReactivity),
                        color = AppColors.Primary,
                        trackColor = AppColors.Background,
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 🌟 FIX: Use the formatted string
                    Text(if (batchResult == null) "--" else "${formatMs(avgSingle)}ms", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Multi-Node (Global)", fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = if (isSimulating || batchResult == null) 0f else (avgMulti.toFloat() / maxReactivity),
                        color = AppColors.Warning,
                        trackColor = AppColors.Background,
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 🌟 FIX: Use the formatted string
                    Text(if (batchResult == null) "--" else "${formatMs(avgMulti)}ms", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🌟 FIX: Accurately calculate the difference using Doubles
                val diff = if (batchResult != null) formatMs(avgMulti - avgSingle) else "0"

                val descText = if (batchResult == null) "Run simulation to calculate ripple effect."
                else if (avgMulti < avgSingle) "Multi-machine resolution runs faster due to localized heuristic checks."
                else "Multi-machine ripple effect adds avg +${diff}ms resolution time."

                Text(descText, fontSize = 11.sp, color = AppColors.TextMuted, lineHeight = 16.sp)
            }
        }
    }
}@Composable
fun StrategicInsightCard(batchResult: BatchResultEvent?, isSimulating: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Blue left accent bar
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(AppColors.Primary))

            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Strategic AI Insight", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isSimulating) {
                    Text("Analyzing operations...", fontSize = 13.sp, color = AppColors.TextMuted)
                } else if (batchResult == null) {
                    Text("Select a configuration above and click 'Run' to generate insights based on the C++ MAS Engine.", fontSize = 13.sp, color = AppColors.TextMuted)
                } else {
                    // Split the generated recommendation to pull out the bold parts if possible,
                    // or just render it directly.
                    Text(
                        text = batchResult.recommendation.replace("**", ""), // Simple strip for now
                        fontSize = 13.sp,
                        color = AppColors.TextMain,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simple pill extraction logic based on fallback text
                    val pillText = if (batchResult.recommendation.contains("Diminishing")) "DIMINISHING RETURNS DETECTED"
                    else if (batchResult.recommendation.contains("bottlenecked")) "BOTTLENECK WARNING"
                    else "OPTIMAL CONFIGURATION"

                    val pillColor = if (pillText.contains("WARNING")) AppColors.Error else AppColors.Secondary

                    Box(modifier = Modifier.background(pillColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("STATUS → $pillText", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pillColor)
                    }
                }
            }
        }
    }
}
// ============================================================
//  🟡 MODIFIED Component: Strategic Insight Card
// ============================================================
