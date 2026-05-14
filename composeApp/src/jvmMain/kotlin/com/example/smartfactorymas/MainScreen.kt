package com.example.smartfactorymas

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
    var anomalyTime by remember { mutableStateOf(8.0) }
    var schedulingStart by remember { mutableStateOf(4.0) }

    var rulMin by remember { mutableStateOf(100.0) }
    var rulProb by remember { mutableStateOf(120.0) }
    var rulMax by remember { mutableStateOf(140.0) }

    var jobs by remember { mutableStateOf(listOf(JobInput("P4", 20.0, 60.0),JobInput("P3", 46.0, 40.0), JobInput("P2", 6.0, 93.0), JobInput("P1", 21.0, 110.0))) }
    var tbms by remember { mutableStateOf(listOf(TbmInput("M1", 70.0, 90.0), TbmInput("M2", 96.0, 103.0))) }
    var arhs by remember { mutableStateOf(listOf(
        ArhUiState("ARH_1", 24.0, 50.0, 4.0, 7.0, 9.0),
        ArhUiState("ARH_2", 103.0, 140.0, 3.0, 6.0, 9.0)
    )) }

    var isAutoRunning by remember { mutableStateOf(false) }
    var selectedProposal by remember { mutableStateOf(0) }

    var ganttState by remember { mutableStateOf(domain.buildInitialState(jobs, tbms, schedulingStart, rulMin, rulMax)) }

    LaunchedEffect(jobs, tbms, schedulingStart, rulMin, rulMax) {
        if (!isAutoRunning && ganttState.masOutput == null) {
            ganttState = domain.buildInitialState(jobs, tbms, schedulingStart, rulMin, rulMax)
        }
    }

    fun runAutoSimulation() {
        if (isAutoRunning) return
        scope.launch {
            isAutoRunning = true
            ganttState = domain.buildInitialState(jobs, tbms, schedulingStart, rulMin, rulMax)
                .copy(logs = emptyList())
            delay(400)
            ganttState = domain.buildAnomalyState(jobs, tbms, schedulingStart, anomalyTime, rulMin, rulMax)
                .copy(logs = ganttState.logs)

            try {
                val initSched = domain.calculateInitialSchedule(jobs, tbms, schedulingStart)

                val futureJobs = jobs.filter { jobInput ->
                    val scheduledJob = initSched.find { it.id == jobInput.id }
                    scheduledJob != null && scheduledJob.startTime > anomalyTime
                }

                val historyJobs = initSched.filter {
                    it.startTime <= anomalyTime && it.type == TaskType.PRODUCTION
                }
                val engineStartTime = if (historyJobs.isNotEmpty())
                    historyJobs.maxOf { it.endTime } else schedulingStart

                // Weights are driven by strategy:
                // SOM → prioritize maintenance (w2 dominant)
                // SOP → prioritize production  (w1 dominant)
                val effectiveW1 = w1.toDouble()
                val effectiveW2 = 1.0 - effectiveW1

                val input = EngineInput(
                    strategy, anomalyTime, engineStartTime,
                    effectiveW1, effectiveW2,
                    rulMin, rulProb, rulMax,
                    futureJobs, tbms,
                    arhs.map { ArhInput(it.id, it.availStart, it.availEnd, it.durMin, it.durProb, it.durMax) }
                )

                val engineRes = withContext(Dispatchers.IO) {
                    domain.runEngine(input) { event ->
                        if (event is LogEvent)
                            ganttState = ganttState.copy(logs = ganttState.logs + event)
                    }
                }

                val out = engineRes.output
                delay(800)

                if (out.proposals.isEmpty()) {
                    val reason = if (strategy == "SOM")
                        "No ARH can finish before RUL.min (t=${rulMin.toInt()}). Try SOP strategy."
                    else
                        "No ARH can finish before RUL.max (t=${rulMax.toInt()}). Machine failure unavoidable."

                    ganttState = ganttState.copy(
                        logs = ganttState.logs + LogEvent(
                            agent = "SYS", msg = "ABORT: $reason", level = "error"
                        )
                    )
                } else {
                    val chosenIdx = out.proposals
                        .indexOfFirst { it.arhId == out.chosenArh }
                        .coerceAtLeast(0)
                    selectedProposal = chosenIdx
                    ganttState = domain.buildResultState(chosenIdx, rulMin, rulMax, initSched, anomalyTime)
                        .copy(logs = ganttState.logs)
                }
            } catch (e: Exception) {
                ganttState = ganttState.copy(
                    logs = ganttState.logs + LogEvent(
                        agent = "SYS", msg = "ERROR: ${e.message}", level = "error"
                    )
                )
            }
            isAutoRunning = false
        }
    }

    var leftExpanded by remember { mutableStateOf(true) }
    var rightExpanded by remember { mutableStateOf(true) }
    var leftBaseWidth by remember { mutableStateOf(300.dp) } // Made wider to accommodate fields
    var rightBaseWidth by remember { mutableStateOf(400.dp) }

    val leftCurrentWidth by animateDpAsState(if (leftExpanded) leftBaseWidth else 48.dp)
    val rightCurrentWidth by animateDpAsState(if (rightExpanded) rightBaseWidth else 48.dp)
    val density = LocalDensity.current

    Row(modifier = Modifier.fillMaxSize().background(SurfaceBright)) {
        Sidebar()

        Column(modifier = Modifier.fillMaxSize().weight(1f)) {
            TopHeader()
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                // LEFT PANEL (Control Panel)
                Column(modifier = Modifier.width(leftCurrentWidth).fillMaxHeight()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = if (leftExpanded) Arrangement.End else Arrangement.Center) {
                        IconButton(onClick = { leftExpanded = !leftExpanded }, modifier = Modifier.size(36.dp)) {
                            Icon(if (leftExpanded) Icons.Default.KeyboardDoubleArrowLeft else Icons.Default.KeyboardDoubleArrowRight, "")
                        }
                    }
                    if (leftExpanded) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            ControlPanel(
                                strategy, { strategy = it }, w1, { w1 = it },
                                anomalyTime, { anomalyTime = it }, schedulingStart, { schedulingStart = it },
                                rulMin, { rulMin = it }, rulProb, { rulProb = it }, rulMax, { rulMax = it },
                                jobs, { jobs = it; ganttState = ganttState.copy(masOutput = null) },
                                tbms, { tbms = it; ganttState = ganttState.copy(masOutput = null) },
                                arhs, { arhs = it },
                                ::runAutoSimulation
                            )
                        }
                    }
                }

                if (leftExpanded) Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(OutlineVariant.copy(0.5f)).pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount -> change.consume(); leftBaseWidth = (leftBaseWidth + with(density) { dragAmount.toDp() }).coerceIn(250.dp, 600.dp) }
                })

                // CENTER PANEL (Gantt)
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ganttState.masOutput?.let { out ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            out.proposals.forEachIndexed { idx, prop ->
                                val isChosen = prop.arhId == out.chosenArh
                                FilterChip(
                                    selected = idx == selectedProposal,
                                    onClick = {
                                        selectedProposal = idx
                                        // PRESERVE the narrative logs when switching tabs by adding .copy(logs = ganttState.logs)
                                        val initSched = domain.calculateInitialSchedule(jobs, tbms, schedulingStart)
                                        ganttState = domain.buildResultState(
                                            idx, rulMin, rulMax, initSched, anomalyTime
                                        ).copy(logs = ganttState.logs)
                                    },
                                    label = { Text("${prop.arhId}${if (isChosen) " ★" else ""}", fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium) }
                                )
                            }
                        }
                    }
                    GanttChart(ganttState, anomalyTime = anomalyTime, {}, {}, isAutoRunning, Modifier.fillMaxSize())
                }

                if (rightExpanded) Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(OutlineVariant.copy(0.5f)).pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount -> change.consume(); rightBaseWidth = (rightBaseWidth - with(density) { dragAmount.toDp() }).coerceIn(250.dp, 600.dp) }
                })

                // RIGHT PANEL (Logs)
                Column(modifier = Modifier.width(rightCurrentWidth).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (rightExpanded) Arrangement.Start else Arrangement.Center) {
                        IconButton(onClick = { rightExpanded = !rightExpanded }, modifier = Modifier.size(36.dp)) {
                            Icon(if (rightExpanded) Icons.Default.KeyboardDoubleArrowRight else Icons.Default.KeyboardDoubleArrowLeft, "")
                        }
                    }
                    if (rightExpanded) {
                        MetricsDashboard(ganttState.f1, ganttState.f2, ganttState.f, ganttState.chosenArh, Modifier.fillMaxWidth())
                        CommunicationLog(ganttState.logs, Modifier.weight(1f).fillMaxWidth())
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
