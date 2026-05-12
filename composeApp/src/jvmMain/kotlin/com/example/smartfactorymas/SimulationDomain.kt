package com.example.smartfactorymas

/**
 * Represents a single block on the Gantt chart.
 */
data class TaskBlock(
    val id: String,
    val type: TaskType,
    val startTime: Double,
    val duration: Double,
    val deadline: Double? = null,
    // Fuzzy bounds for visual rendering
    val startMin: Double = startTime,
    val startMax: Double = startTime,
    val endMin: Double = startTime + duration,
    val endMax: Double = startTime + duration
) {
    val endTime: Double get() = startTime + duration
}

enum class TaskType { PRODUCTION, TBM, CBM }

/**
 * Full state for the Gantt + metrics display.
 */
data class GanttState(
    val currentStep: Int = 0,
    val schedule: List<TaskBlock> = emptyList(),
    val f1: Float = 0f,
    val f2: Float = 0f,
    val f: Float = 0f,
    val logs: List<String> = emptyList(),
    val chosenArh: String? = null,
    val masOutput: MASOutput? = null,
    val selectedProposalIdx: Int = 0,
    val tracks: List<List<TaskBlock>> = emptyList() // Added to hold multiple rows/steps
)

/**
 * Domain logic — bridges engine output to UI state.
 */
class SimulationDomain {

    private val anomalyTime = 8.0

    // Initial schedule (before anomaly, Table 4.1)
    val initialSchedule = listOf(
        TaskBlock("P4", TaskType.PRODUCTION, 4.0, 20.0, 60.0),
        TaskBlock("P3", TaskType.PRODUCTION, 24.0, 46.0, 40.0),
        TaskBlock("M1", TaskType.TBM, 70.0, 20.0),
        TaskBlock("P2", TaskType.PRODUCTION, 90.0, 6.0, 93.0),
        TaskBlock("M2", TaskType.TBM, 96.0, 7.0),
        TaskBlock("P1", TaskType.PRODUCTION, 103.0, 21.0, 110.0)
    )

    private var cachedResult: CliInterop.EngineResult? = null

    fun buildInitialState(): GanttState = GanttState(
        currentStep = 0,
        schedule = initialSchedule,
        tracks = listOf(initialSchedule),
        logs = listOf("[AMS] Initial schedule loaded. Monitoring machine health...")
    )

    fun buildAnomalyState(): GanttState = GanttState(
        currentStep = 1,
        schedule = initialSchedule,
        tracks = listOf(initialSchedule),
        logs = listOf(
            "[AMS] Initial schedule loaded.",
            "[AMC] ANOMALY DETECTED at t=$anomalyTime!",
            "[AMC] Diagnostic: CBM_Required",
            "[AMS] Forwarding to ASRH for negotiation..."
        )
    )

    /**
     * Converts engine output into a GanttState for a specific proposal.
     */
    fun buildResultState(proposalIdx: Int = 0): GanttState {
        val result = cachedResult ?: return buildInitialState()
        val out = result.output
        val proposals = out.proposals
        if (proposals.isEmpty()) return buildInitialState()

        val idx = proposalIdx.coerceIn(0, proposals.lastIndex)
        val prop = proposals[idx]

        val schedule = prop.schedule.map { block ->
            val type = when (block.type) {
                "PRODUCTION" -> TaskType.PRODUCTION
                "TBM" -> TaskType.TBM
                "CBM" -> TaskType.CBM
                else -> TaskType.PRODUCTION
            }
            TaskBlock(
                id = block.id,
                type = type,
                startTime = block.startProb,
                duration = block.endProb - block.startProb,
                deadline = if (block.type == "PRODUCTION") block.dueDate else null,
                startMin = block.startMin,
                startMax = block.startMax,
                endMin = block.endMin,
                endMax = block.endMax
            )
        }

        // Build construction steps (tracks) for the Gantt Chart
        val fixedBlocks = schedule.filter { it.type == TaskType.CBM || it.type == TaskType.TBM }
        val prodJobs = schedule.filter { it.type == TaskType.PRODUCTION }
        val tracks = mutableListOf<List<TaskBlock>>()
        
        if (prop.arhId == "ARH_1") {
            // Figure 4.6 implementation
            tracks.add(initialSchedule) // (0)
            tracks.add(fixedBlocks)     // (1)
            
            // Track 2: CBM + M1 + M2 + P3(103-149) + P2(149-155) + P1(155-176)
            val p3_end = TaskBlock("P3", TaskType.PRODUCTION, 103.0, 46.0, 40.0)
            val p2_end = TaskBlock("P2", TaskType.PRODUCTION, 149.0, 6.0, 93.0)
            val p1_end = TaskBlock("P1", TaskType.PRODUCTION, 155.0, 21.0, 110.0)
            tracks.add(fixedBlocks + listOf(p3_end, p2_end, p1_end)) // (2)
            
            // Track 3: P1 moved to 31-52, P3 still at end
            val p1_correct = prodJobs.find { it.id == "P1" } ?: p1_end
            val p3_correct = prodJobs.find { it.id == "P3" } ?: p3_end
            tracks.add(fixedBlocks + listOf(p1_correct, p3_correct)) // (3)
            
            tracks.add(schedule) // (4) Final
        } else if (prop.arhId == "ARH_2") {
            // Figure 4.7 implementation
            tracks.add(initialSchedule) // (0)
            
            val tbmOnly = fixedBlocks.filter { it.type == TaskType.TBM }
            val cbm = fixedBlocks.find { it.type == TaskType.CBM }
            val p3 = initialSchedule.find { it.id == "P3" }!!
            val p2 = initialSchedule.find { it.id == "P2" }!!
            
            // Track 1: M1, M2, P3, P2
            tracks.add(tbmOnly + listOf(p3, p2)) // (1)
            
            // Track 2: M1, M2, P3, P2, CBM
            if (cbm != null) tracks.add(tbmOnly + listOf(p3, p2, cbm)) // (2)
            
            tracks.add(schedule) // (3) Final
        } else {
            // Fallback sequential logic
            tracks.add(initialSchedule)
            tracks.add(fixedBlocks)
            var currentTrack = fixedBlocks.toMutableList()
            for (job in prodJobs) {
                currentTrack = currentTrack.toMutableList()
                currentTrack.add(job)
                tracks.add(currentTrack)
            }
        }

        return GanttState(
            currentStep = 3,
            schedule = schedule,
            tracks = tracks,
            f1 = prop.f1Prob.toFloat(),
            f2 = prop.f2.toFloat(),
            f = prop.fProb.toFloat(),
            logs = result.logs.filter { it.isNotBlank() },
            chosenArh = out.chosenArh,
            masOutput = out,
            selectedProposalIdx = idx
        )
    }

    /**
     * Runs the C++ engine with the given configuration.
     */
    fun runEngine(
        input: EngineInput? = null,
        onLogLine: (String) -> Unit = {}
    ): CliInterop.EngineResult {
        val result = CliInterop.runSimulation(input, onLogLine)
        cachedResult = result
        return result
    }

    fun hasResult() = cachedResult != null
    fun proposalCount() = cachedResult?.output?.proposals?.size ?: 0
}
