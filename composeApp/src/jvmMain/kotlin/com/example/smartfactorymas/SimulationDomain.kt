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
    val logs: List<LogEvent> = emptyList(),
    val chosenArh: String? = null,
    val masOutput: MASOutput? = null,
    val selectedProposalIdx: Int = 0,
    val tracks: List<List<TaskBlock>> = emptyList()
)

/**
 * Domain logic — bridges engine output to UI state.
 */

class SimulationDomain {

    private val anomalyTime = 8.0

    val initialSchedule = listOf(
        TaskBlock("P4", TaskType.PRODUCTION, 4.0, 20.0, 60.0),
        TaskBlock("P3", TaskType.PRODUCTION, 24.0, 46.0, 40.0),
        TaskBlock("M1", TaskType.TBM, 70.0, 20.0),
        TaskBlock("P2", TaskType.PRODUCTION, 90.0, 6.0, 93.0),
        TaskBlock("M2", TaskType.TBM, 96.0, 7.0),
        TaskBlock("P1", TaskType.PRODUCTION, 103.0, 21.0, 110.0)
    )

    private var cachedResult: CliInterop.EngineResult? = null

    // FIXED: Using LogEvent instead of plain Strings
    fun buildInitialState(): GanttState = GanttState(
        currentStep = 0,
        schedule = initialSchedule,
        tracks = listOf(initialSchedule),
        logs = listOf(
            LogEvent(agent = "AMS", msg = "Initial schedule loaded. Monitoring machine health...", level = "info")
        )
    )

    // FIXED: Using LogEvent instead of plain Strings
    fun buildAnomalyState(): GanttState = GanttState(
        currentStep = 1,
        schedule = initialSchedule,
        tracks = listOf(initialSchedule),
        logs = listOf(
            LogEvent(agent = "AMS", msg = "Initial schedule loaded.", level = "info"),
            LogEvent(agent = "AMC", msg = "ANOMALY DETECTED at t=$anomalyTime!", level = "error"),
            LogEvent(agent = "AMC", msg = "Diagnostic: CBM_Required", level = "warn"),
            LogEvent(agent = "AMS", msg = "Forwarding to ASRH for negotiation...", level = "info")
        )
    )

    fun buildResultState(proposalIdx: Int = 0): GanttState {
        val result = cachedResult ?: return buildInitialState()
        val out = result.output
        val proposals = out.proposals
        if (proposals.isEmpty()) return buildInitialState()

        val idx = proposalIdx.coerceIn(0, proposals.lastIndex)
        val prop = proposals[idx]

        // Map final engine result
        val finalEngineSchedule = prop.schedule.map { block ->
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

        val cbmBlock = finalEngineSchedule.find { it.type == TaskType.CBM } ?: return buildInitialState()
        val cbmStart = cbmBlock.startTime
        val cbmDuration = cbmBlock.duration

        val finalFixedBlocks = finalEngineSchedule.filter { it.type == TaskType.CBM || it.type == TaskType.TBM }

        // Jobs that finished before CBM (like P4)

        // Original order (P3 -> P2 -> P1)
        val originalAffectedJobs = initialSchedule.filter {
            it.type == TaskType.PRODUCTION && it.endTime > cbmStart
        }.sortedBy { it.startTime }

        val tracks = mutableListOf<List<TaskBlock>>()

        // Track 0: Initial
        tracks.add(initialSchedule)

        // Track 1: CBM Inserted
        tracks.add(finalFixedBlocks)

        // Track 2: Naive Sequential Push
        var cursor = cbmStart + cbmDuration
        val naiveJobs = mutableListOf<TaskBlock>()
        for (job in originalAffectedJobs) {
            var collision: TaskBlock?
            do {
                val currentEnd = cursor + job.duration
                collision = finalFixedBlocks.find {
                    it.type == TaskType.TBM && cursor < it.endTime && currentEnd > it.startTime
                }
                if (collision != null) cursor = collision.endTime
            } while (collision != null)

            naiveJobs.add(job.copy(
                startTime = cursor, startMin = cursor, startMax = cursor,
                endMin = cursor + job.duration, endMax = cursor + job.duration
            ))
            cursor += job.duration
        }
        tracks.add( finalFixedBlocks + naiveJobs)

        // Track 3..N: Pulling to Optimal
        val optimalAffectedJobs = finalEngineSchedule.filter {
            it.type == TaskType.PRODUCTION && it.id in originalAffectedJobs.map { j -> j.id }
        }.sortedBy { it.startTime }

        var currentTrackJobs = naiveJobs.toList()
        for (optimizedJob in optimalAffectedJobs) {
            currentTrackJobs = currentTrackJobs.map {
                if (it.id == optimizedJob.id) optimizedJob else it
            }
            tracks.add( finalFixedBlocks + currentTrackJobs)
        }

        val completeFinalSchedule =  finalEngineSchedule
        if (tracks.last() != completeFinalSchedule) {
            tracks.add(completeFinalSchedule)
        }

        return GanttState(
            currentStep = tracks.lastIndex,
            schedule = completeFinalSchedule,
            tracks = tracks,
            f1 = prop.f1Prob.toFloat(),
            f2 = prop.f2.toFloat(),
            f = prop.fProb.toFloat(),
            // FIXED: result.logs is already List<LogEvent>, removed the invalid string filter
            logs = result.logs,
            chosenArh = out.chosenArh,
            masOutput = out,
            selectedProposalIdx = idx
        )
    }

    // FIXED: Changed callback from (String) -> Unit to (EngineEvent) -> Unit to match CliInterop
    fun runEngine(input: EngineInput? = null, onEvent: (EngineEvent) -> Unit = {}): CliInterop.EngineResult {
        val result = CliInterop.runSimulation(input, onEvent)
        cachedResult = result
        return result
    }

    fun hasResult() = cachedResult != null
    fun proposalCount() = cachedResult?.output?.proposals?.size ?: 0
}
