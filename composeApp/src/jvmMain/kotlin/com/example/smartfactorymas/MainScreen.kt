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
import androidx.compose.ui.draw.clip
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

    var jobs by remember { mutableStateOf(listOf(JobInput("P4", 20.0, 60.0), JobInput("P3", 46.0, 40.0), JobInput("P2", 6.0, 93.0), JobInput("P1", 21.0, 110.0))) }
    var tbms by remember { mutableStateOf(listOf(TbmInput("M1", 70.0, 90.0), TbmInput("M2", 96.0, 103.0))) }
    var arhs by remember { mutableStateOf(listOf(
        ArhUiState("ARH_1", 24.0, 50.0, 4.0, 7.0, 9.0),
        ArhUiState("ARH_2", 103.0, 140.0, 3.0, 6.0, 9.0)
    )) }

    var isAutoRunning by remember { mutableStateOf(false) }
    var selectedProposal by remember { mutableIntStateOf(0) }

    var ganttState by remember { mutableStateOf(domain.buildInitialState(jobs, tbms, schedulingStart, rulMin, rulMax)) }

    // 🌟 الدالة السحرية للتحكم بالخطوات 🌟
    fun applyStepState(step: Int, proposalIdx: Int = selectedProposal) {
        val initSched = domain.calculateInitialSchedule(jobs, tbms, schedulingStart)
        val baseState = ganttState.copy(currentStep = step)

        // 🌟 استخراج نتائج الـ f1, f2, f لكي تظهر في الشاشة بدلاً من الأصفار 🌟
        val metricsState = if (baseState.masOutput != null && baseState.masOutput.proposals.isNotEmpty()) {
            domain.buildResultState(proposalIdx, rulMin, rulMax, initSched, anomalyTime)
        } else {
            baseState
        }

        fun mapToTaskBlocks(dtos: List<ScheduleBlockDto>?): List<TaskBlock>? {
            if (dtos == null) return null
            return dtos.map { dto ->
                TaskBlock(
                    id = dto.id,
                    type = when (dto.type) {
                        "CBM" -> TaskType.CBM
                        "TBM" -> TaskType.TBM
                        else -> TaskType.PRODUCTION
                    },
                    startTime = dto.startProb,
                    duration = dto.endProb - dto.startProb,
                    startMin = dto.startMin,
                    startMax = dto.startMax,
                    endMin = dto.endMin,
                    endMax = dto.endMax,
                    deadline = if (dto.dueDate > 0.0) dto.dueDate else null
                )
            }
        }

        ganttState = when (step) {
            0 -> domain.buildInitialState(jobs, tbms, schedulingStart, rulMin, rulMax)
                .copy(currentStep = 0, masOutput = baseState.masOutput, logs = baseState.logs)
            1 -> domain.buildAnomalyState(jobs, tbms, schedulingStart, anomalyTime, rulMin, rulMax)
                .copy(currentStep = 1, masOutput = baseState.masOutput, logs = baseState.logs)
            2 -> { // 🌟 STEP 2: Fixed Obstacles 🌟
                val prop = baseState.masOutput?.proposals?.getOrNull(proposalIdx)
                val fixedBlocks = initSched.filter { it.type == TaskType.TBM }.toMutableList()
                val cbmDto = prop?.tracks?.getOrNull(0)?.find { it.type == "CBM" || it.id == "CBM" }
                if (cbmDto != null) {
                    fixedBlocks.add(
                        TaskBlock(
                            id = cbmDto.id, type = TaskType.CBM,
                            startTime = cbmDto.startProb, duration = cbmDto.endProb - cbmDto.startProb,
                            startMin = cbmDto.startMin, startMax = cbmDto.startMax,
                            endMin = cbmDto.endMin, endMax = cbmDto.endMax, deadline = null
                        )
                    )
                }
                baseState.copy(
                    schedule = fixedBlocks, tracks = listOf(fixedBlocks),
                    // تمرير القيم هنا
                    f1 = metricsState.f1, f2 = metricsState.f2, f = metricsState.f, chosenArh = metricsState.chosenArh
                )
            }
            3 -> { // 🌟 STEP 3: Naive Right-Shift 🌟
                val prop = baseState.masOutput?.proposals?.getOrNull(proposalIdx)
                val naiveTrack = mapToTaskBlocks(prop?.tracks?.getOrNull(0)) ?: initSched
                baseState.copy(
                    schedule = naiveTrack, tracks = listOf(naiveTrack),
                    // تمرير القيم هنا
                    f1 = metricsState.f1, f2 = metricsState.f2, f = metricsState.f, chosenArh = metricsState.chosenArh
                )
            }
            4 -> { // 🌟 STEP 4: Final Optimal Result (Show ALL Steps) 🌟
                val prop = baseState.masOutput?.proposals?.getOrNull(proposalIdx)

                val fixedBlocks = initSched.filter { it.type == TaskType.TBM }.toMutableList()
                val cbmDto = prop?.tracks?.getOrNull(0)?.find { it.type == "CBM" || it.id == "CBM" }
                if (cbmDto != null) {
                    fixedBlocks.add(
                        TaskBlock(
                            id = cbmDto.id, type = TaskType.CBM,
                            startTime = cbmDto.startProb, duration = cbmDto.endProb - cbmDto.startProb,
                            startMin = cbmDto.startMin, startMax = cbmDto.startMax,
                            endMin = cbmDto.endMin, endMax = cbmDto.endMax, deadline = null
                        )
                    )
                }
                val naiveTrack = mapToTaskBlocks(prop?.tracks?.getOrNull(0)) ?: initSched
                val optimalTrack = mapToTaskBlocks(prop?.tracks?.getOrNull(1) ?: prop?.schedule) ?: initSched

                baseState.copy(
                    schedule = optimalTrack, tracks = listOf(initSched, fixedBlocks, naiveTrack, optimalTrack),
                    // تمرير القيم هنا لتعود الحياة للأرقام!
                    f1 = metricsState.f1, f2 = metricsState.f2, f = metricsState.f, chosenArh = metricsState.chosenArh
                )
            }
            else -> baseState
        }
    }

    LaunchedEffect(jobs, tbms, schedulingStart, rulMin, rulMax) {
        if (!isAutoRunning && ganttState.masOutput == null) {
            applyStepState(0)
        }
    }

    fun runAutoSimulation() {
        if (isAutoRunning) return
        scope.launch {
            isAutoRunning = true
            ganttState = ganttState.copy(masOutput = null, logs = emptyList())

            // Step 0: Initial
            applyStepState(0)
            delay(500)

            // Step 1: Anomaly
            applyStepState(1)

            try {
                val initSched = domain.calculateInitialSchedule(jobs, tbms, schedulingStart)
                val futureJobs = jobs.filter { jobInput ->
                    val scheduledJob = initSched.find { it.id == jobInput.id }
                    scheduledJob != null && scheduledJob.startTime > anomalyTime
                }

                val historyJobs = initSched.filter { it.startTime <= anomalyTime && it.type == TaskType.PRODUCTION }
                val engineStartTime = if (historyJobs.isNotEmpty()) historyJobs.maxOf { it.endTime } else schedulingStart

                val input = EngineInput(
                    strategy, anomalyTime, engineStartTime, w1.toDouble(), 1.0 - w1.toDouble(),
                    rulMin, rulProb, rulMax, futureJobs, tbms,
                    arhs.map { ArhInput(it.id, it.availStart, it.availEnd, it.durMin, it.durProb, it.durMax) }
                )

                val engineRes = withContext(Dispatchers.IO) {
                    domain.runEngine(input) { event ->
                        if (event is LogEvent) ganttState = ganttState.copy(logs = ganttState.logs + event)
                    }
                }

                val out = engineRes.output
                delay(800)

                if (out.proposals.isEmpty()) {
                    ganttState = ganttState.copy(logs = ganttState.logs + LogEvent(agent = "SYS", msg = "ABORT: No valid proposals.", level = "error"))
                } else {
                    val chosenIdx = out.proposals.indexOfFirst { it.arhId == out.chosenArh }.coerceAtLeast(0)
                    selectedProposal = chosenIdx
                    ganttState = ganttState.copy(masOutput = out) // حفظ النتائج أولاً

                    // 🌟 Animated Steps for Presentation 🌟
                    delay(800)
                    applyStepState(2, chosenIdx) // Show Naive
                    delay(1200)
                    applyStepState(3, chosenIdx) // Show Comparison
                    delay(1500)
                    applyStepState(4, chosenIdx) // Show Final
                }
            } catch (e: Exception) {
                ganttState = ganttState.copy(logs = ganttState.logs + LogEvent(agent = "SYS", msg = "ERROR: ${e.message}", level = "error"))
            }
            isAutoRunning = false
        }
    }

    var leftExpanded by remember { mutableStateOf(true) }
    var rightExpanded by remember { mutableStateOf(true) }
    var leftBaseWidth by remember { mutableStateOf(300.dp) }
    var rightBaseWidth by remember { mutableStateOf(400.dp) }

    val leftCurrentWidth by animateDpAsState(if (leftExpanded) leftBaseWidth else 48.dp)
    val rightCurrentWidth by animateDpAsState(if (rightExpanded) rightBaseWidth else 48.dp)
    val density = LocalDensity.current

    Row(modifier = Modifier.fillMaxSize().background(SurfaceBright)) {
        Sidebar()

        Column(modifier = Modifier.fillMaxSize().weight(1f)) {
            TopHeader()

            Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                // LEFT PANEL
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
                                arhs, { arhs = it }, ::runAutoSimulation
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
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            out.proposals.forEachIndexed { idx, prop ->
                                val isChosen = prop.arhId == out.chosenArh
                                FilterChip(
                                    selected = idx == selectedProposal,
                                    onClick = {
                                        selectedProposal = idx
                                        applyStepState(ganttState.currentStep, idx) // 👈 يحافظ على الخطوة الحالية عند تبديل العمال
                                    },
                                    label = { Text("${prop.arhId}${if (isChosen) " ★" else ""}", fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryContainer,
                                        selectedLabelColor = OnPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    GanttChart(
                        state = ganttState,
                        anomalyTime = anomalyTime,
                        onPreviousStep = { if (ganttState.currentStep > 0) applyStepState(ganttState.currentStep - 1) },
                        onNextStep = { if (ganttState.currentStep < 4) applyStepState(ganttState.currentStep + 1) },
                        isAutoRunning = isAutoRunning,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (rightExpanded) Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(OutlineVariant.copy(0.5f)).pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount -> change.consume(); rightBaseWidth = (rightBaseWidth - with(density) { dragAmount.toDp() }).coerceIn(250.dp, 600.dp) }
                })

                // RIGHT PANEL
                Column(modifier = Modifier.width(rightCurrentWidth).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
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

@Composable
fun Sidebar() {
    Column(
        modifier = Modifier.fillMaxHeight().width(100.dp).background(SurfaceContainerLow).border(1.dp, OutlineVariant).padding(vertical = 24.dp)
    ) {
        SidebarItem("", Icons.Default.Dashboard, true)
        SidebarItem("", Icons.Default.Timeline, false)
        SidebarItem("", Icons.Default.Engineering, false)
        SidebarItem("", Icons.Default.PrecisionManufacturing, false)
        SidebarItem("", Icons.Default.Settings, false)
    }
}

@Composable
fun SidebarItem(title: String, icon: ImageVector, isSelected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .background(if (isSelected) PrimaryContainer else Color.Transparent, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isSelected) OnPrimaryContainer else OnSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = if (isSelected) OnPrimaryContainer else OnSurfaceVariant, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
fun TopHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(SurfaceBright).border(1.dp, OutlineVariant).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Maintenance Scheduler", color = Primary, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        }
    }
}