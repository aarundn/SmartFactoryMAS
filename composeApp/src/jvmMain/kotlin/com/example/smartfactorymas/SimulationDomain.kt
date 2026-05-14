package com.example.smartfactorymas

/**
 * Represents a single block on the Gantt chart.
 */
data class TaskBlock(
    val id: String, val type: TaskType, val startTime: Double, val duration: Double,
    val deadline: Double? = null,
    val startMin: Double = startTime, val startMax: Double = startTime,
    val endMin: Double = startTime + duration, val endMax: Double = startTime + duration
) { val endTime: Double get() = startTime + duration }

enum class TaskType { PRODUCTION, TBM, CBM }

data class GanttState(
    val currentStep: Int = 0,
    val schedule: List<TaskBlock> = emptyList(),
    val f1: Float = 0f, val f2: Float = 0f, val f: Float = 0f,
    val logs: List<LogEvent> = emptyList(),
    val chosenArh: String? = null,
    val masOutput: MASOutput? = null,
    val multiMachineOutput: MultiMachineResultEvent? = null, // 🌟 الحقل المفقود الضروري لحفظ حالة الآلات المتعددة
    val selectedProposalIdx: Int = 0,
    val tracks: List<List<TaskBlock>> = emptyList(),
    val rulMin: Double = 100.0, val rulMax: Double = 140.0
)

class SimulationDomain {
    private var cachedResult: CliInterop.EngineResult? = null

    fun calculateInitialSchedule(jobs: List<JobInput>, tbms: List<TbmInput>, schedulingStart: Double): List<TaskBlock> {
        val result   = mutableListOf<TaskBlock>()
        var current  = schedulingStart
        val sortedTbms = tbms.sortedBy { it.start }
        var tbmIndex = 0

        fun insertDueTbms() {
            while (tbmIndex < sortedTbms.size && current >= sortedTbms[tbmIndex].start - 0.001) {
                val tbm = sortedTbms[tbmIndex]
                result.add(TaskBlock(tbm.id, TaskType.TBM, tbm.start, tbm.end - tbm.start))
                current = tbm.end; tbmIndex++
            }
        }
        for (job in jobs) {
            while (true) {
                insertDueTbms()
                val blockEnd = current + job.duration
                if (tbmIndex < sortedTbms.size && blockEnd > sortedTbms[tbmIndex].start + 0.001) {
                    val tbm = sortedTbms[tbmIndex]
                    result.add(TaskBlock(tbm.id, TaskType.TBM, tbm.start, tbm.end - tbm.start))
                    current = tbm.end; tbmIndex++; continue
                }
                result.add(TaskBlock(job.id, TaskType.PRODUCTION, current, job.duration, job.dueDate))
                current = blockEnd; break
            }
        }
        insertDueTbms()
        while (tbmIndex < sortedTbms.size) {
            val tbm = sortedTbms[tbmIndex]
            result.add(TaskBlock(tbm.id, TaskType.TBM, tbm.start, tbm.end - tbm.start))
            current = tbm.end; tbmIndex++
        }
        return result
    }

    fun buildInitialState(jobs: List<JobInput>, tbms: List<TbmInput>, start: Double, rulMin: Double, rulMax: Double): GanttState {
        val schedule = calculateInitialSchedule(jobs, tbms, start)
        return GanttState(currentStep = 0, schedule = schedule, tracks = listOf(schedule), rulMin = rulMin, rulMax = rulMax)
    }

    fun buildAnomalyState(jobs: List<JobInput>, tbms: List<TbmInput>, start: Double, anomalyTime: Double, rulMin: Double, rulMax: Double): GanttState {
        val schedule = calculateInitialSchedule(jobs, tbms, start)
        return GanttState(currentStep = 1, schedule = schedule, tracks = listOf(schedule), rulMin = rulMin, rulMax = rulMax)
    }

    fun buildResultState(proposalIdx: Int = 0, rulMin: Double, rulMax: Double, initialSchedule: List<TaskBlock>, anomalyTime: Double): GanttState {
        val result = cachedResult ?: return GanttState()

        // 🌟 الإصلاح: التعامل الآمن مع out الذي قد يكون null 🌟
        val out = result.output
        if (out.proposals.isEmpty()) return GanttState(logs = result.logs, multiMachineOutput = result.multiMachineResult)

        val idx  = proposalIdx.coerceIn(0, out.proposals.lastIndex)
        val prop = out.proposals[idx]

        fun mapBlock(b: ScheduleBlockDto) = TaskBlock(b.id,
            when (b.type) { "PRODUCTION" -> TaskType.PRODUCTION; "TBM" -> TaskType.TBM; "CBM" -> TaskType.CBM; else -> TaskType.PRODUCTION },
            b.startProb, b.endProb - b.startProb,
            if (b.type == "PRODUCTION") b.dueDate else null, b.startMin, b.startMax, b.endMin, b.endMax)

        val optimal = prop.schedule.map { mapBlock(it) }
        val naive   = prop.tracks.getOrNull(0)?.map { mapBlock(it) } ?: emptyList()
        val history = initialSchedule.filter { it.startTime <= anomalyTime && optimal.none { b -> b.id == it.id } }
        val fixed   = optimal.filter { it.type == TaskType.TBM || it.type == TaskType.CBM || (it.type == TaskType.PRODUCTION && it.endTime <= prop.cbmStart) }

        return GanttState(
            currentStep = 4, schedule = history + optimal,
            tracks = listOf(initialSchedule, fixed, if (naive.isNotEmpty()) naive else fixed, optimal),
            f1 = prop.f1Prob.toFloat(), f2 = prop.f2.toFloat(), f = prop.fProb.toFloat(),
            logs = result.logs, chosenArh = out.chosenArh, masOutput = out,
            multiMachineOutput = result.multiMachineResult, // 🌟 تمرير نتيجة الآلات المتعددة
            selectedProposalIdx = idx, rulMin = rulMin, rulMax = rulMax)
    }

    fun runEngine(input: EngineInput? = null, onEvent: (EngineEvent) -> Unit = {}): CliInterop.EngineResult {
        val result = CliInterop.runSimulation(input, onEvent); cachedResult = result; return result
    }
}