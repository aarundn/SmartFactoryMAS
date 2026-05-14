package com.example.smartfactorymas

/**
 * Represents a single block on the Gantt chart.
 */
data class TaskBlock(
    val id: String, val type: TaskType, val startTime: Double, val duration: Double,
    val deadline: Double? = null,
    val startMin: Double = startTime, val startMax: Double = startTime,
    val endMin: Double = startTime + duration, val endMax: Double = startTime + duration
) {
    val endTime: Double get() = startTime + duration
}

enum class TaskType { PRODUCTION, TBM, CBM }

data class GanttState(
    val currentStep: Int = 0,
    val schedule: List<TaskBlock> = emptyList(),
    val f1: Float = 0f, val f2: Float = 0f, val f: Float = 0f,
    val logs: List<LogEvent> = emptyList(),
    val chosenArh: String? = null,
    val masOutput: MASOutput? = null,
    val selectedProposalIdx: Int = 0,
    val tracks: List<List<TaskBlock>> = emptyList(),
    val rulMin: Double = 100.0,
    val rulMax: Double = 140.0
)

class SimulationDomain {
    private var cachedResult: CliInterop.EngineResult? = null

    fun calculateInitialSchedule(
        jobs: List<JobInput>, tbms: List<TbmInput>, schedulingStart: Double
    ): List<TaskBlock> {
        val result = mutableListOf<TaskBlock>()
        var current = schedulingStart
        val sortedTbms = tbms.sortedBy { it.start }
        var tbmIndex = 0

        fun insertDueTbms() {
            while (tbmIndex < sortedTbms.size && current >= sortedTbms[tbmIndex].start - 0.001) {
                val tbm = sortedTbms[tbmIndex]
                result.add(TaskBlock(tbm.id, TaskType.TBM, tbm.start, tbm.end - tbm.start))
                current = tbm.end
                tbmIndex++
            }
        }

        for (job in jobs) {
            while (true) {
                insertDueTbms()
                val blockEnd = current + job.duration
                if (tbmIndex < sortedTbms.size && blockEnd > sortedTbms[tbmIndex].start + 0.001) {
                    val tbm = sortedTbms[tbmIndex]
                    result.add(TaskBlock(tbm.id, TaskType.TBM, tbm.start, tbm.end - tbm.start))
                    current = tbm.end
                    tbmIndex++
                    continue
                }
                result.add(TaskBlock(job.id, TaskType.PRODUCTION, current, job.duration, job.dueDate))
                current = blockEnd
                break
            }
        }

        insertDueTbms()
        while (tbmIndex < sortedTbms.size) {
            val tbm = sortedTbms[tbmIndex]
            result.add(TaskBlock(tbm.id, TaskType.TBM, tbm.start, tbm.end - tbm.start))
            current = tbm.end
            tbmIndex++
        }
        return result
    }

    fun buildInitialState(jobs: List<JobInput>, tbms: List<TbmInput>, start: Double, rulMin: Double, rulMax: Double): GanttState {
        val schedule = calculateInitialSchedule(jobs, tbms, start)
        return GanttState(
            currentStep = 0, schedule = schedule, tracks = listOf(schedule),
            rulMin = rulMin, rulMax = rulMax, logs = emptyList()
        )
    }

    fun buildAnomalyState(jobs: List<JobInput>, tbms: List<TbmInput>, start: Double, anomalyTime: Double, rulMin: Double, rulMax: Double): GanttState {
        val schedule = calculateInitialSchedule(jobs, tbms, start)
        return GanttState(
            currentStep = 1, schedule = schedule, tracks = listOf(schedule),
            rulMin = rulMin, rulMax = rulMax, logs = emptyList()
        )
    }

    // تم إضافة anomalyTime كمعلمة هنا لتطبيق فكرتك الديناميكية
    fun buildResultState(proposalIdx: Int = 0, rulMin: Double, rulMax: Double, initialSchedule: List<TaskBlock>, anomalyTime: Double): GanttState {
        val result = cachedResult ?: return GanttState()
        val out = result.output
        val proposals = out.proposals
        if (proposals.isEmpty()) return GanttState()

        val idx = proposalIdx.coerceIn(0, proposals.lastIndex)
        val prop = proposals[idx]
        val cbmStart = prop.cbmStart

        fun mapEngineBlock(block: ScheduleBlockDto): TaskBlock {
            val type = when (block.type) {
                "PRODUCTION" -> TaskType.PRODUCTION
                "TBM" -> TaskType.TBM
                "CBM" -> TaskType.CBM
                else -> TaskType.PRODUCTION
            }
            return TaskBlock(
                id = block.id, type = type, startTime = block.startProb, duration = block.endProb - block.startProb,
                deadline = if (block.type == "PRODUCTION") block.dueDate else null,
                startMin = block.startMin, startMax = block.startMax, endMin = block.endMin, endMax = block.endMax
            )
        }

        // 1. استلام الجداول الجاهزة من محرك C++
        val optimalEngineBlocks = prop.schedule.map { mapEngineBlock(it) }
        val naiveEngineBlocks = prop.tracks.getOrNull(0)?.map { mapEngineBlock(it) } ?: emptyList()
        print("optimalEngineBlocks: $optimalEngineBlocks\nnaiveEngineBlocks: $naiveEngineBlocks")
        // 2. الكتل التاريخية (مثل P4 التي انتهت قبل العطل) للرسم فقط
        val historyBlocks = initialSchedule.filter { initialBlock ->
            initialBlock.startTime <= anomalyTime && optimalEngineBlocks.none { it.id == initialBlock.id }
        }

        val tracks = mutableListOf<List<TaskBlock>>()

        // الخطوة (0): الجدول الأصلي
        tracks.add(initialSchedule)

        // الخطوة (1): الثوابت فقط (CBM + TBMs)

        val fixedBlocks = optimalEngineBlocks.filter {
            it.type == TaskType.TBM || it.type == TaskType.CBM || (it.type == TaskType.PRODUCTION && it.endTime <= cbmStart)
        }
        tracks.add(fixedBlocks)


        // الخطوة (2): الإزاحة لليمين (Right-Shift) للوظائف الإنتاجية
        if (naiveEngineBlocks.isNotEmpty()) {
            tracks.add( naiveEngineBlocks)
        } else {
            tracks.add(fixedBlocks) // Fallback
        }

        // =========================================================
        // الخطوة (3): الخطوة الأخيرة (الترتيب المثالي Final Optimal)
        // =========================================================
        tracks.add(optimalEngineBlocks)

        return GanttState(
            currentStep = tracks.lastIndex,
            schedule = historyBlocks + optimalEngineBlocks,
            tracks = tracks,
            f1 = prop.f1Prob.toFloat(), f2 = prop.f2.toFloat(), f = prop.fProb.toFloat(),
            logs = result.logs, chosenArh = out.chosenArh, masOutput = out, selectedProposalIdx = idx,
            rulMin = rulMin, rulMax = rulMax
        )
    }

    fun runEngine(input: EngineInput? = null, onEvent: (EngineEvent) -> Unit = {}): CliInterop.EngineResult {
        val result = CliInterop.runSimulation(input, onEvent)
        cachedResult = result
        return result
    }
}