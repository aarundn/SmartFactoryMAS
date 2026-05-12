package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen() {
    val domain = remember { SimulationDomain() }
    val scope = rememberCoroutineScope()

    var strategy by remember { mutableStateOf("SOM") }
    var w1 by remember { mutableStateOf(0.75f) }
    var anomalyTime by remember { mutableStateOf(8) }
    var severity by remember { mutableStateOf(0.7f) }
    var cbmBaseDur by remember { mutableStateOf(7) }
    var arhs by remember {
        mutableStateOf(listOf(
            ArhUiState("ARH_1", 24.0, 50.0, 4.0, 7.0, 9.0),
            ArhUiState("ARH_2", 103.0, 140.0, 3.0, 6.0, 9.0)
        ))
    }

    var ganttState by remember { mutableStateOf(domain.buildInitialState()) }
    var isAutoRunning by remember { mutableStateOf(false) }
    var selectedProposal by remember { mutableStateOf(0) }

    fun buildEngineInput(): EngineInput = EngineInput(
        alertTime = anomalyTime.toDouble(),
        schedulingStart = 24.0,
        w1 = w1.toDouble(),
        w2 = (1.0 - w1).toDouble(),
        jobs = listOf(
            JobInput("P3", 46.0, 40.0),
            JobInput("P2", 6.0, 93.0),
            JobInput("P1", 21.0, 110.0)
        ),
        tbmBlocks = listOf(
            TbmInput("M1", 70.0, 90.0),
            TbmInput("M2", 96.0, 103.0)
        ),
        arhAgents = arhs.map { ArhInput(it.id, it.availStart, it.availEnd, it.durMin, it.durProb, it.durMax) }
    )

    fun runAutoSimulation() {
        if (isAutoRunning) return
        scope.launch {
            isAutoRunning = true

            ganttState = domain.buildAnomalyState()
            delay(1000)

            try {
                val input = buildEngineInput()
                withContext(Dispatchers.IO) {
                    domain.runEngine(input)
                }
                selectedProposal = 0
                ganttState = domain.buildResultState(0)
            } catch (e: Exception) {
                ganttState = ganttState.copy(
                    logs = ganttState.logs + "[ERROR] ${e.message}"
                )
            }
            isAutoRunning = false
        }
    }

    var leftExpanded by remember { mutableStateOf(true) }
    var rightExpanded by remember { mutableStateOf(true) }
    var leftBaseWidth by remember { mutableStateOf(200.dp) }
    var rightBaseWidth by remember { mutableStateOf(400.dp) }
    
    val leftCurrentWidth by animateDpAsState(if (leftExpanded) leftBaseWidth else 48.dp)
    val rightCurrentWidth by animateDpAsState(if (rightExpanded) rightBaseWidth else 48.dp)
    val density = LocalDensity.current

    Row(modifier = Modifier.fillMaxSize().background(SurfaceBright)) {
        Sidebar()

        Column(modifier = Modifier.fillMaxSize().weight(1f)) {
            TopHeader()

            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Left: Control Panel ──
                Column(modifier = Modifier.width(leftCurrentWidth).fillMaxHeight()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = if (leftExpanded) Arrangement.End else Arrangement.Center
                    ) {
                        IconButton(onClick = { leftExpanded = !leftExpanded }, modifier = Modifier.size(36.dp)) {
                            Icon(if (leftExpanded) Icons.Default.KeyboardDoubleArrowLeft else Icons.Default.KeyboardDoubleArrowRight, "Toggle Left Panel")
                        }
                    }
                    if (leftExpanded) {
                        val scrollState = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            ControlPanel(
                                selectedStrategy = strategy,
                                onStrategyChange = { strategy = it },
                                w1 = w1,
                                onW1Change = { w1 = it },
                                anomalyTime = anomalyTime,
                                onAnomalyTimeChange = { anomalyTime = it },
                                anomalySeverity = severity,
                                onSeverityChange = { severity = it },
                                cbmBaseDuration = cbmBaseDur,
                                onCbmBaseChange = { cbmBaseDur = it },
                                arhs = arhs,
                                onArhsChange = { arhs = it },
                                onSimulateClick = ::runAutoSimulation
                            )
                        }
                    }
                }

                // Splitter Left
                if (leftExpanded) {
                    Box(modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(OutlineVariant.copy(0.5f))
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                val dragDp = with(density) { dragAmount.toDp() }
                                leftBaseWidth = (leftBaseWidth + dragDp).coerceIn(250.dp, 600.dp)
                            }
                        }
                    )
                }

                // ── Center: Gantt Chart with proposal tabs ──
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Proposal selector tabs (if we have results)
                    val output = ganttState.masOutput
                    if (output != null && output.proposals.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            output.proposals.forEachIndexed { idx, prop ->
                                val isChosen = prop.arhId == output.chosenArh
                                val isSelected = idx == selectedProposal
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedProposal = idx
                                        ganttState = domain.buildResultState(idx)
                                    },
                                    label = {
                                        Text(
                                            "${prop.arhId}${if (isChosen) " ★" else ""}",
                                            fontSize = 13.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isChosen) PrimaryContainer else SecondaryContainer
                                    )
                                )
                            }
                        }
                    }

                    GanttChart(
                        state = ganttState,
                        onPreviousStep = {},
                        onNextStep = {},
                        isAutoRunning = isAutoRunning,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Splitter Right
                if (rightExpanded) {
                    Box(modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(OutlineVariant.copy(0.5f))
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                val dragDp = with(density) { dragAmount.toDp() }
                                rightBaseWidth = (rightBaseWidth - dragDp).coerceIn(250.dp, 600.dp)
                            }
                        }
                    )
                }

                // ── Right: Metrics + Logs ──
                Column(
                    modifier = Modifier.width(rightCurrentWidth).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (rightExpanded) Arrangement.Start else Arrangement.Center
                    ) {
                        IconButton(onClick = { rightExpanded = !rightExpanded }, modifier = Modifier.size(36.dp)) {
                            Icon(if (rightExpanded) Icons.Default.KeyboardDoubleArrowRight else Icons.Default.KeyboardDoubleArrowLeft, "Toggle Right Panel")
                        }
                    }
                    if (rightExpanded) {
                        MetricsDashboard(
                            f1 = ganttState.f1,
                            f2 = ganttState.f2,
                            globalF = ganttState.f,
                            chosenArh = ganttState.chosenArh,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        CommunicationLog(
                            logs = ganttState.logs,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ── Sidebar ──────────────────────────────────────────────────────────────────

@Composable
fun Sidebar() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(256.dp)
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant)
            .padding(vertical = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Fleet Control", color = Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Active Ops", color = OnSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
        SidebarItem("Dashboard", Icons.Default.Dashboard, true)
        SidebarItem("Timeline", Icons.Default.Timeline, false)
        SidebarItem("Technicians", Icons.Default.Engineering, false)
        SidebarItem("Assets", Icons.Default.PrecisionManufacturing, false)
        SidebarItem("Configuration", Icons.Default.Settings, false)
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.padding(24.dp)) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Maintenance Task", color = OnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun SidebarItem(title: String, icon: ImageVector, isSelected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(if (isSelected) PrimaryContainer else Color.Transparent, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isSelected) OnPrimaryContainer else OnSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = if (isSelected) OnPrimaryContainer else OnSurfaceVariant, fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

// ── Top Header ───────────────────────────────────────────────────────────────

@Composable
fun TopHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .background(SurfaceBright).border(1.dp, OutlineVariant)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Maintenance Scheduler", color = Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(24.dp))
            OutlinedTextField(
                value = "", onValueChange = {},
                placeholder = { Text("Search operations...", color = OnSurfaceVariant, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.width(320.dp).height(40.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceContainerLow,
                    focusedContainerColor = SurfaceContainerLowest,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Primary
                )
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = {}) { Icon(Icons.Outlined.Notifications, null, tint = OnSurfaceVariant) }
            IconButton(onClick = {}) { Icon(Icons.Outlined.Settings, null, tint = OnSurfaceVariant) }
            IconButton(onClick = {}) { Icon(Icons.Outlined.HelpOutline, null, tint = OnSurfaceVariant) }
            Box(Modifier.size(32.dp).background(SurfaceVariant, CircleShape).border(1.dp, OutlineVariant, CircleShape))
        }
    }
}
