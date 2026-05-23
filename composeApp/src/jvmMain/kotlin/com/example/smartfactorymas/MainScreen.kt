package com.example.smartfactorymas

import CommunicationLog
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfactorymas.ui.MaintenanceSchedulerRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen() {
    val domain = remember { SimulationDomain() }
    val scope  = rememberCoroutineScope()

    // ─── Local-AMS state ───────────────────────────────────────────────────
    var strategy        by remember { mutableStateOf("SOM") }
    var w1              by remember { mutableStateOf(0.75f) }
    var anomalyTime     by remember { mutableStateOf(8.0) }
    var schedulingStart by remember { mutableStateOf(4.0) }
    var rulMin          by remember { mutableStateOf(100.0) }
    var rulProb         by remember { mutableStateOf(120.0) }
    var rulMax          by remember { mutableStateOf(140.0) }

    var jobs by remember { mutableStateOf(listOf(
        JobInput("P4", 20.0, 60.0), JobInput("P3", 46.0, 40.0),
        JobInput("P2", 6.0,  93.0), JobInput("P1", 21.0, 110.0))) }
    var tbms by remember { mutableStateOf(listOf(TbmInput("M1", 70.0, 90.0), TbmInput("M2", 96.0, 103.0))) }
    var arhs by remember { mutableStateOf(listOf(
        ArhUiState("ARH_1", 24.0, 50.0, 4.0, 7.0, 9.0),
        ArhUiState("ARH_2", 103.0, 140.0, 3.0, 6.0, 9.0))) }

    var isAutoRunning    by remember { mutableStateOf(false) }
    var selectedProposal by remember { mutableIntStateOf(0) }
    var ganttState       by remember { mutableStateOf(domain.buildInitialState(jobs, tbms, schedulingStart, rulMin, rulMax)) }

    // ─── Global-factory state ──────────────────────────────────────────────
    var selectedTab by remember { mutableStateOf("local") }  // "local" | "global" | "advanced_labs"
    var amaConfig   by remember { mutableStateOf(AmaUiConfig()) }
    var amvConfig   by remember { mutableStateOf(AmvUiConfig()) }
    var mmState     by remember { mutableStateOf(MultiMachineState()) }
    var isMmRunning by remember { mutableStateOf(false) }

    // ─── Layout toggles ───────────────────────────────────────────────────
    var leftExpanded      by remember { mutableStateOf(true) }
    var rightExpanded     by remember { mutableStateOf(true) }
    var leftBaseWidth     by remember { mutableStateOf(300.dp) }
    var rightBaseWidth    by remember { mutableStateOf(400.dp) }
    val leftCurrentWidth  by animateDpAsState(if (leftExpanded) leftBaseWidth else 48.dp)
    val rightCurrentWidth by animateDpAsState(if (rightExpanded) rightBaseWidth else 48.dp)
    val density = LocalDensity.current

    // ─── Multi-Machine Step Logic ─────────────────────────────────────────
    fun applyMultiMachineStep(step: Int) {
        fun mapSchedule(dtos: List<ScheduleBlockDto>?): List<TaskBlock> {
            return dtos?.map { dto ->
                TaskBlock(
                    id = dto.id,
                    type = if (dto.type == "CBM") TaskType.CBM else if (dto.type == "TBM") TaskType.TBM else TaskType.PRODUCTION,
                    startTime = dto.startProb,
                    duration = dto.endProb - dto.startProb
                )
            } ?: emptyList()
        }

        val amsLocalSchedule = mapSchedule(ganttState.masOutput?.chosenProposal()?.schedule)
        val amsGlobalSchedule = mapSchedule(ganttState.multiMachineOutput?.amsSchedule).ifEmpty { amsLocalSchedule }

        val amaSchedule = amaConfig.blocks.map { TaskBlock(it.jobId, TaskType.PRODUCTION, it.start, it.end - it.start) }
        val amvOriginalSchedule = amvConfig.blocks.map { TaskBlock(it.jobId, TaskType.PRODUCTION, it.start, it.end - it.start) }.sortedBy { it.startTime }

        val allMessages = ganttState.multiMachineOutput?.messages ?: emptyList()

        // 🌟 خوارزمية الإزاحة الحقيقية (Ripple Effect) لمنع تداخل المهام في AMV 🌟
        val amvShiftedSchedule = mutableListOf<TaskBlock>()
        var lastEndTime = 0.0

        for (block in amvOriginalSchedule) {
            val delayMsg = allMessages.find { it.type == "I_MESSAGE" && it.jobId == block.id }
            var newStart = block.startTime

            if (delayMsg != null && delayMsg.requestedTime > newStart) {
                newStart = delayMsg.requestedTime
            }

            if (newStart < lastEndTime) {
                newStart = lastEndTime
            }

            val newDuration = block.duration
            val newEnd = newStart + newDuration

            amvShiftedSchedule.add(block.copy(startTime = newStart))
            lastEndTime = newEnd
        }

        val baseMmState = mmState.copy(
            amaSchedule = amaSchedule,
            amvOriginalSchedule = amvOriginalSchedule
        )

        mmState = when (step) {
            0 -> baseMmState.copy(currentStep = step, amsSchedule = amsLocalSchedule, amvSchedule = amvOriginalSchedule, amsStatus = MachineStatus.STEADY, amaStatus = MachineStatus.STEADY, amvStatus = MachineStatus.STEADY)
            1 -> baseMmState.copy(currentStep = step, amsSchedule = amsLocalSchedule, amvSchedule = amvOriginalSchedule, amsStatus = MachineStatus.ANOMALY)
            2 -> baseMmState.copy(currentStep = step, amsSchedule = amsLocalSchedule, amvSchedule = amvOriginalSchedule, amsStatus = MachineStatus.RESCHEDULING)
            3 -> baseMmState.copy(
                currentStep = step,
                amsSchedule = amsLocalSchedule,
                amvSchedule = amvOriginalSchedule,
                messages = allMessages.filter { it.type == "M_MESSAGE" },
                amsStatus = MachineStatus.CONFLICT,
                amaStatus = if (ganttState.multiMachineOutput?.upstreamConflict == true) MachineStatus.CONFLICT else MachineStatus.STEADY
            )
            4 -> baseMmState.copy(
                currentStep = step,
                amsSchedule = amsGlobalSchedule,
                amvSchedule = amvShiftedSchedule,
                messages = allMessages,
                amsStatus = MachineStatus.DONE,
                amaStatus = if (ganttState.multiMachineOutput?.upstreamConflict == true) MachineStatus.RESOLVED else MachineStatus.STEADY,
                amvStatus = if (amvShiftedSchedule != amvOriginalSchedule) MachineStatus.SHIFTED else MachineStatus.STEADY
            )
            else -> baseMmState
        }
    }

    fun applyStepState(step: Int, proposalIdx: Int = selectedProposal) {
        val initSched   = domain.calculateInitialSchedule(jobs, tbms, schedulingStart)
        val baseState   = ganttState.copy(currentStep = step)

        // 🌟 THE FIX: Extract the metrics directly from the selected proposal! 🌟
        val currentProp = baseState.masOutput?.proposals?.getOrNull(proposalIdx)
        val currentF1 = (currentProp?.f1Prob ?: 0.0).toFloat()
        val currentF2 = (currentProp?.f2 ?: 0.0).toFloat()
        val currentF = (currentProp?.fProb ?: 0.0).toFloat()
        val currentChosenArh = currentProp?.arhId ?: ""

        fun mapDto(dtos: List<ScheduleBlockDto>?) = dtos?.map { dto ->
            TaskBlock(dto.id,
                when (dto.type) { "CBM" -> TaskType.CBM; "TBM" -> TaskType.TBM; else -> TaskType.PRODUCTION },
                dto.startProb, dto.endProb - dto.startProb,
                if (dto.dueDate > 0.0) dto.dueDate else null,
                dto.startMin, dto.startMax, dto.endMin, dto.endMax)
        }

        ganttState = when (step) {
            0 -> domain.buildInitialState(jobs, tbms, schedulingStart, rulMin, rulMax)
                .copy(currentStep = 0, masOutput = baseState.masOutput, logs = baseState.logs, multiMachineOutput = baseState.multiMachineOutput)
            1 -> domain.buildAnomalyState(jobs, tbms, schedulingStart, anomalyTime, rulMin, rulMax)
                .copy(currentStep = 1, masOutput = baseState.masOutput, logs = baseState.logs, multiMachineOutput = baseState.multiMachineOutput)
            2 -> {
                val fixed    = initSched.filter { it.type == TaskType.TBM }.toMutableList()
                val cbmDto   = currentProp?.tracks?.getOrNull(0)?.find { it.type == "CBM" || it.id == "CBM" }
                if (cbmDto != null) fixed.add(TaskBlock(cbmDto.id, TaskType.CBM, cbmDto.startProb,
                    cbmDto.endProb - cbmDto.startProb, null, cbmDto.startMin, cbmDto.startMax, cbmDto.endMin, cbmDto.endMax))
                baseState.copy(schedule = fixed, tracks = listOf(fixed),
                    f1 = currentF1, f2 = currentF2, f = currentF, chosenArh = currentChosenArh)
            }
            3 -> {
                val naive = mapDto(currentProp?.tracks?.getOrNull(0)) ?: initSched
                baseState.copy(schedule = naive, tracks = listOf(naive),
                    f1 = currentF1, f2 = currentF2, f = currentF, chosenArh = currentChosenArh)
            }
            4 -> {
                val fixed       = initSched.filter { it.type == TaskType.TBM }.toMutableList()
                val cbmDto      = currentProp?.tracks?.getOrNull(0)?.find { it.type == "CBM" || it.id == "CBM" }
                if (cbmDto != null) fixed.add(TaskBlock(cbmDto.id, TaskType.CBM, cbmDto.startProb,
                    cbmDto.endProb - cbmDto.startProb, null, cbmDto.startMin, cbmDto.startMax, cbmDto.endMin, cbmDto.endMax))
                val naive   = mapDto(currentProp?.tracks?.getOrNull(0)) ?: initSched
                val optimal = mapDto(currentProp?.tracks?.getOrNull(1) ?: currentProp?.schedule) ?: initSched
                baseState.copy(schedule = optimal, tracks = listOf(initSched, fixed, naive, optimal),
                    f1 = currentF1, f2 = currentF2, f = currentF, chosenArh = currentChosenArh)
            }
            else -> baseState
        }
    }

    LaunchedEffect(jobs, tbms, schedulingStart, rulMin, rulMax) {
        if (!isAutoRunning && ganttState.masOutput == null) applyStepState(0)
    }

    fun runLocalSimulation() {
        if (isAutoRunning) return
        scope.launch {
            isAutoRunning = true
            ganttState = ganttState.copy(masOutput = null, multiMachineOutput = null, logs = emptyList())
            applyStepState(0); delay(500); applyStepState(1)
            try {
                val initSched    = domain.calculateInitialSchedule(jobs, tbms, schedulingStart)
                val futureJobs   = jobs.filter { j ->
                    (initSched.find { it.id == j.id }?.startTime ?: 0.0) > anomalyTime
                }
                val historyJobs  = initSched.filter { it.startTime <= anomalyTime && it.type == TaskType.PRODUCTION }
                val engStart     = if (historyJobs.isNotEmpty()) historyJobs.maxOf { it.endTime } else schedulingStart

                val input = EngineInput(
                    mode = "single",
                    strategy = strategy,
                    alertTime = anomalyTime,
                    schedulingStart = engStart,
                    w1 = w1.toDouble(),
                    w2 = 1.0 - w1.toDouble(),
                    rulMin = rulMin,
                    rulProb = rulProb,
                    rulMax = rulMax,
                    jobs = futureJobs,
                    tbmBlocks = tbms,
                    arhAgents = arhs.map { ArhInput(it.id, it.availStart, it.availEnd, it.durMin, it.durProb, it.durMax) }
                )

                val res = withContext(Dispatchers.IO) {
                    domain.runEngine(input) { ev -> if (ev is LogEvent) ganttState = ganttState.copy(logs = ganttState.logs + ev) }
                }
                val out = res.output; delay(800)
                if (out?.proposals.isNullOrEmpty()) {
                    ganttState = ganttState.copy(logs = ganttState.logs + LogEvent(agent = "SYS", msg = "ABORT: No valid proposals.", level = "error"))
                } else {
                    val chosenIdx = out!!.proposals.indexOfFirst { it.arhId == out.chosenArh }.coerceAtLeast(0)
                    selectedProposal = chosenIdx; ganttState = ganttState.copy(masOutput = out)
                    delay(800); applyStepState(2, chosenIdx)
                    delay(1200); applyStepState(3, chosenIdx)
                    delay(1500); applyStepState(4, chosenIdx)
                }
            } catch (e: Exception) {
                ganttState = ganttState.copy(logs = ganttState.logs + LogEvent(agent = "SYS", msg = "ERROR: ${e.message}", level = "error"))
            }
            isAutoRunning = false
        }
    }

    fun runMultiMachineSimulation() {
        if (isMmRunning) return
        scope.launch {
            isMmRunning = true
            mmState = mmState.copy(logs = emptyList())

            val initSched  = domain.calculateInitialSchedule(jobs, tbms, schedulingStart)
            val futureJobs = jobs.filter { j -> initSched.find { it.id == j.id }?.startTime ?: 0.0 > anomalyTime }
            val historyJobs= initSched.filter { it.startTime <= anomalyTime && it.type == TaskType.PRODUCTION }
            val engStart   = if (historyJobs.isNotEmpty()) historyJobs.maxOf { it.endTime } else schedulingStart

            val jobLinks = amaConfig.blocks.map {
                JobLinkInput(jobId = it.jobId, readyTime = it.end)
            }

            val amvJobLinks = amvConfig.blocks.map {
                AmvJobLinkInput(jobId = it.jobId, expectedStartOnNext = it.start)
            }

            val input = EngineInput(
                mode = "multi",
                strategy = strategy,
                alertTime = anomalyTime,
                schedulingStart = engStart,
                w1 = w1.toDouble(),
                w2 = 1.0 - w1.toDouble(),
                rulMin = rulMin,
                rulProb = rulProb,
                rulMax = rulMax,
                jobs = futureJobs,
                tbmBlocks = tbms,
                arhAgents = arhs.map { ArhInput(it.id, it.availStart, it.availEnd, it.durMin, it.durProb, it.durMax) },

                amaId = amaConfig.id,
                amaSchedule = amaConfig.blocks.map { NeighborBlockInput(it.jobId, "PRODUCTION", it.start, it.end) },
                jobLinks = jobLinks,

                amvId = amvConfig.id,
                amvSchedule = amvConfig.blocks.map { NeighborBlockInput(it.jobId, "PRODUCTION", it.start, it.end) },
                amvJobLinks = amvJobLinks
            )

            val res = withContext(Dispatchers.IO) {
                domain.runEngine(input) { ev ->
                    if (ev is LogEvent) mmState = mmState.copy(logs = mmState.logs + ev)
                }
            }

            ganttState = ganttState.copy(
                masOutput = res.output ?: ganttState.masOutput,
                multiMachineOutput = res.multiMachineResult
            )

            delay(800)
            if (res.multiMachineResult != null) {
                applyMultiMachineStep(0); delay(800)
                applyMultiMachineStep(1); delay(1200)
                applyMultiMachineStep(2); delay(1200)
                applyMultiMachineStep(3); delay(1500)
                applyMultiMachineStep(4)
            } else {
                mmState = mmState.copy(logs = mmState.logs + LogEvent("SYS", "Failed to get multi-machine output", "error"))
            }

            isMmRunning = false
        }
    }

    // ─── Layout ────────────────────────────────────────────────────────────
    Row(modifier = Modifier.fillMaxSize().background(SurfaceBright)) {
        Sidebar(selectedTab) { selectedTab = it }

        Column(modifier = Modifier.fillMaxSize().weight(1f)) {
            TopHeader(selectedTab) { selectedTab = it }

            // 🌟 استخدام when بدلاً من if/else للتنقل بين الشاشات 🌟
            when (selectedTab) {
                "local" -> {
                    // ── LOCAL AMS VIEW ─────────────
                    Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                        Column(modifier = Modifier.width(leftCurrentWidth).fillMaxHeight()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = if (leftExpanded) Arrangement.End else Arrangement.Center) {
                                IconButton(onClick = { leftExpanded = !leftExpanded }, modifier = Modifier.size(36.dp)) {
                                    Icon(if (leftExpanded) Icons.Default.KeyboardDoubleArrowLeft else Icons.Default.KeyboardDoubleArrowRight, "")
                                }
                            }
                            if (leftExpanded) {
                                val localScrollState = rememberScrollState()
                                Column(modifier = Modifier.verticalScroll(localScrollState).keyboardAndCursorScroll(localScrollState, scope)) {
                                    ControlPanel(
                                        strategy, { strategy = it }, w1, { w1 = it },
                                        anomalyTime, { anomalyTime = it }, schedulingStart, { schedulingStart = it },
                                        rulMin, { rulMin = it }, rulProb, { rulProb = it }, rulMax, { rulMax = it },
                                        jobs, { jobs = it; ganttState = ganttState.copy(masOutput = null) },
                                        tbms, { tbms = it; ganttState = ganttState.copy(masOutput = null) },
                                        arhs, { arhs = it }, ::runLocalSimulation)
                                }
                            }
                        }

                        if (leftExpanded) Box(modifier = Modifier.fillMaxHeight().width(4.dp)
                            .background(OutlineVariant.copy(0.5f))
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { ch, dx -> ch.consume()
                                    leftBaseWidth = (leftBaseWidth + with(density) { dx.toDp() }).coerceIn(250.dp, 600.dp) }
                            })

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            ganttState.masOutput?.let { out ->
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    out.proposals.forEachIndexed { idx, prop ->
                                        val isChosen = prop.arhId == out.chosenArh
                                        FilterChip(selected = idx == selectedProposal,
                                            onClick = { selectedProposal = idx; applyStepState(ganttState.currentStep, idx) },
                                            label = { Text("${prop.arhId}${if (isChosen) " ★" else ""}", fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryContainer, selectedLabelColor = OnPrimaryContainer))
                                    }
                                }
                            }
                            GanttChart(state = ganttState, anomalyTime = anomalyTime,
                                onPreviousStep = { if (ganttState.currentStep > 0) applyStepState(ganttState.currentStep - 1) },
                                onNextStep     = { if (ganttState.currentStep < 4) applyStepState(ganttState.currentStep + 1) },
                                isAutoRunning  = isAutoRunning, modifier = Modifier.fillMaxSize())
                        }

                        if (rightExpanded) Box(modifier = Modifier.fillMaxHeight().width(4.dp)
                            .background(OutlineVariant.copy(0.5f))
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { ch, dx -> ch.consume()
                                    rightBaseWidth = (rightBaseWidth - with(density) { dx.toDp() }).coerceIn(250.dp, 600.dp) }
                            })

                        Column(modifier = Modifier.width(rightCurrentWidth).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (rightExpanded) Arrangement.Start else Arrangement.Center) {
                                IconButton(onClick = { rightExpanded = !rightExpanded }, modifier = Modifier.size(36.dp)) {
                                    Icon(if (rightExpanded) Icons.Default.KeyboardDoubleArrowRight else Icons.Default.KeyboardDoubleArrowLeft, "")
                                }
                            }
                            if (rightExpanded) {
                                MetricsDashboard(ganttState.f1.toFloat(), ganttState.f2.toFloat(), ganttState.f.toFloat(), ganttState.chosenArh, Modifier.fillMaxWidth())
                                CommunicationLog(ganttState.logs, Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                } // End of local view

                "global" -> {
                    // ── GLOBAL FACTORY VIEW ────────────────────────────────
                    Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Left: multi-machine control panel
                        Column(modifier = Modifier.width(leftCurrentWidth).fillMaxHeight()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = if (leftExpanded) Arrangement.End else Arrangement.Center) {
                                IconButton(onClick = { leftExpanded = !leftExpanded }, modifier = Modifier.size(36.dp)) {
                                    Icon(if (leftExpanded) Icons.Default.KeyboardDoubleArrowLeft else Icons.Default.KeyboardDoubleArrowRight, "")
                                }
                            }
                            if (leftExpanded) {
                                val globalScrollState = rememberScrollState()
                                Column(modifier = Modifier.verticalScroll(globalScrollState).keyboardAndCursorScroll(globalScrollState, scope)) {
                                    MultiMachineControlPanel(
                                        mmState       = mmState,
                                        amaConfig     = amaConfig,
                                        amvConfig     = amvConfig,
                                        isRunning     = isMmRunning,
                                        hasLocalResult = true,
                                        onMmStateChange = { mmState = it },
                                        onAmaChange   = { amaConfig = it },
                                        onAmvChange   = { amvConfig = it },
                                        onSimulate    = ::runMultiMachineSimulation
                                    )
                                }
                            }
                        }

                        if (leftExpanded) Box(modifier = Modifier.fillMaxHeight().width(4.dp)
                            .background(OutlineVariant.copy(0.5f))
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { ch, dx -> ch.consume()
                                    leftBaseWidth = (leftBaseWidth + with(density) { dx.toDp() }).coerceIn(250.dp, 600.dp) }
                            })

                        // Center: 3-lane Gantt
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            MultiMachineGanttChart(
                                state      = mmState,
                                onPrevStep = { if (mmState.currentStep > 0) applyMultiMachineStep(mmState.currentStep - 1) },
                                onNextStep = { if (mmState.currentStep < 4 && !isMmRunning) applyMultiMachineStep(mmState.currentStep + 1) },
                                isRunning  = isMmRunning,
                                modifier   = Modifier.fillMaxSize()
                            )
                        }

                        if (rightExpanded) Box(modifier = Modifier.fillMaxHeight().width(4.dp)
                            .background(OutlineVariant.copy(0.5f))
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { ch, dx -> ch.consume()
                                    rightBaseWidth = (rightBaseWidth - with(density) { dx.toDp() }).coerceIn(250.dp, 600.dp) }
                            })

                        // Right: Communication log
                        Column(modifier = Modifier.width(rightCurrentWidth).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (rightExpanded) Arrangement.Start else Arrangement.Center) {
                                IconButton(onClick = { rightExpanded = !rightExpanded }, modifier = Modifier.size(36.dp)) {
                                    Icon(if (rightExpanded) Icons.Default.KeyboardDoubleArrowRight else Icons.Default.KeyboardDoubleArrowLeft, "")
                                }
                            }
                            if (rightExpanded) {
                                SystemStatusCard(mmState, Modifier.fillMaxWidth())
                                CommunicationLog(mmState.logs, Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                } // End of global view

                "advanced_labs" -> {
                    // ── ADVANCED LABS VIEW ────────────────────────────────
                    MaintenanceSchedulerRoot()
                }
            } // End of when
        }
    }
}

// ─── Sidebar ───────────────────────────────────────────────────────────────
@Composable
fun Sidebar(selectedTab: String, onTabSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight().width(100.dp)
            .background(SurfaceContainerLow).border(1.dp, OutlineVariant)
            .padding(vertical = 24.dp)
    ) {
        SidebarItem("", Icons.Default.Dashboard, selectedTab == "local") { onTabSelect("local") }
        SidebarItem("", Icons.Default.AccountTree, selectedTab == "global") { onTabSelect("global") }
        SidebarItem("", Icons.Default.Science, selectedTab == "advanced_labs") { onTabSelect("advanced_labs") }
    }
}

@Composable
fun SidebarItem(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .background(if (isSelected) PrimaryContainer else Color.Transparent, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (isSelected) OnPrimaryContainer else OnSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, color = if (isSelected) OnPrimaryContainer else OnSurfaceVariant,
            fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

// ─── TopHeader ─────────────────────────────────────────────────────────────
@Composable
fun TopHeader(selectedTab: String, onTabChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .background(SurfaceBright).border(1.dp, OutlineVariant)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Maintenance Scheduler", color = Primary, fontSize = 24.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)

        Row(
            modifier = Modifier.background(SurfaceContainerHighest, RoundedCornerShape(8.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            HeaderTab("Local (AMS)",      selectedTab == "local")  { onTabChange("local") }
            HeaderTab("Global Factory",   selectedTab == "global") { onTabChange("global") }
            HeaderTab("Advanced Labs",    selectedTab == "advanced_labs") { onTabChange("advanced_labs") }
        }
    }
}

@Composable
private fun HeaderTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(if (selected) SurfaceContainerLowest else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) OnSurface else OnSurfaceVariant,
            fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}