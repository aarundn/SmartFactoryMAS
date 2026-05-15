package com.example.smartfactorymas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class BatchStability(
    val stable: Double,
    val improved: Double,
    val deteriorated: Double
)

@Serializable
data class BatchReactivity(
    val jobs: Int,
    @SerialName("single_ms") val singleMs: Double,
    @SerialName("multi_ms") val multiMs: Double
)

@Serializable
data class BatchResultEvent(
    override val type: String = "batch_result",
    val stability: BatchStability,
    val recommendation: String,
    val reactivity: List<BatchReactivity>,
    @SerialName("csv_data") val csvData: String
) : EngineEvent() // إذا كنت تستخدم sealed class للأحداث
// ─── Engine events ─────────────────────────────────────────────────────────

@Serializable
sealed class EngineEvent { abstract val type: String }

@Serializable @SerialName("log")
data class LogEvent(
    override val type: String = "log",
    val agent: String, val msg: String,
    val level: String = "info", val step: Int? = null
) : EngineEvent()

@Serializable @SerialName("proposal")
data class ProposalEvent(
    override val type: String = "proposal",
    @SerialName("arh_id") val arhId: String,
    @SerialName("cbm_start") val cbmStart: Double,
    @SerialName("cbm_dur_min") val cbmDurMin: Double,
    @SerialName("cbm_dur_prob") val cbmDurProb: Double,
    @SerialName("cbm_dur_max") val cbmDurMax: Double,
    @SerialName("f1_min") val f1Min: Double,
    @SerialName("f1_prob") val f1Prob: Double,
    @SerialName("f1_max") val f1Max: Double,
    val f2: Double,
    @SerialName("f_min") val fMin: Double,
    @SerialName("f_prob") val fProb: Double,
    @SerialName("f_max") val fMax: Double,
    val schedule: List<ScheduleBlockDto>,
    val tracks: List<List<ScheduleBlockDto>> = emptyList()
) : EngineEvent()

@Serializable @SerialName("result")
data class ResultEvent(
    override val type: String = "result",
    @SerialName("chosen_arh") val chosenArh: String,
    val w1: Double, val w2: Double,
    @SerialName("alert_time") val alertTime: Double,
    val proposals: List<ProposalDto>
) : EngineEvent()

/** Emitted by C++ MultiMachineCoordinator after negotiation */
@Serializable @SerialName("multi_machine_result")
data class MultiMachineResultEvent(
    override val type: String = "multi_machine_result",
    @SerialName("ama_id") val amaId: String = "",
    @SerialName("amv_id") val amvId: String = "",
    val messages: List<NegotiationMessage> = emptyList(),
    @SerialName("upstream_conflict") val upstreamConflict: Boolean = false,
    @SerialName("downstream_conflict") val downstreamConflict: Boolean = false,

    // 🌟 🌟 🌟 الحقول المفقودة التي أضفناها هنا! 🌟 🌟 🌟
    @SerialName("ams_schedule") val amsSchedule: List<ScheduleBlockDto> = emptyList(),
    @SerialName("ama_schedule") val amaSchedule: List<ScheduleBlockDto> = emptyList(),
    @SerialName("amv_schedule") val amvSchedule: List<ScheduleBlockDto> = emptyList()
) : EngineEvent()

// ─── Negotiation message (M_MESSAGE / I_MESSAGE) ───────────────────────────

@Serializable
data class NegotiationMessage(
    val type: String,            // "M_MESSAGE" or "I_MESSAGE"
    val from: String,
    val to: String,
    @SerialName("job_id") val jobId: String,
    @SerialName("original_time") val originalTime: Double,
    @SerialName("requested_time") val requestedTime: Double,
    val accepted: Boolean = true
)

// ─── DTOs (unchanged) ──────────────────────────────────────────────────────

@Serializable
data class ScheduleBlockDto(
    val id: String, val type: String,
    @SerialName("start_min") val startMin: Double = 0.0,
    @SerialName("start_prob") val startProb: Double = 0.0,
    @SerialName("start_max") val startMax: Double = 0.0,
    @SerialName("end_min") val endMin: Double = 0.0,
    @SerialName("end_prob") val endProb: Double = 0.0,
    @SerialName("end_max") val endMax: Double = 0.0,
    @SerialName("due_date") val dueDate: Double = 0.0
)

@Serializable
data class ProposalDto(
    @SerialName("arh_id") val arhId: String,
    @SerialName("cbm_start") val cbmStart: Double,
    @SerialName("cbm_dur_min") val cbmDurMin: Double,
    @SerialName("cbm_dur_prob") val cbmDurProb: Double,
    @SerialName("cbm_dur_max") val cbmDurMax: Double,
    @SerialName("f1_min") val f1Min: Double,
    @SerialName("f1_prob") val f1Prob: Double,
    @SerialName("f1_max") val f1Max: Double,
    val f2: Double,
    @SerialName("f_min") val fMin: Double,
    @SerialName("f_prob") val fProb: Double,
    @SerialName("f_max") val fMax: Double,
    val schedule: List<ScheduleBlockDto>,
    val tracks: List<List<ScheduleBlockDto>> = emptyList()
)

@Serializable
data class MASOutput(
    @SerialName("chosen_arh") val chosenArh: String,
    val w1: Double, val w2: Double,
    @SerialName("alert_time") val alertTime: Double,
    val proposals: List<ProposalDto>
) { fun chosenProposal(): ProposalDto? = proposals.find { it.arhId == chosenArh } }

// ─── Engine input (UPDATED: mode + multi-machine fields) ───────────────────

@Serializable
data class EngineInput(
    val mode: String = "single",          // "single" | "multi"
    val strategy: String = "SOM",
    @SerialName("alert_time") val alertTime: Double = 8.0,
    @SerialName("scheduling_start") val schedulingStart: Double = 24.0,
    val w1: Double = 0.75, val w2: Double = 0.25,
    @SerialName("rul_min") val rulMin: Double = 100.0,
    @SerialName("rul_prob") val rulProb: Double = 120.0,
    @SerialName("rul_max") val rulMax: Double = 140.0,
    val jobs: List<JobInput> = emptyList(),
    @SerialName("tbm_blocks") val tbmBlocks: List<TbmInput> = emptyList(),
    @SerialName("arh_agents") val arhAgents: List<ArhInput> = emptyList(),
    // ── multi-machine ──
    @SerialName("ama_id") val amaId: String? = null,
    @SerialName("ama_schedule") val amaSchedule: List<NeighborBlockInput> = emptyList(),
    @SerialName("job_links") val jobLinks: List<JobLinkInput> = emptyList(),
    @SerialName("amv_id") val amvId: String? = null,
    @SerialName("amv_schedule") val amvSchedule: List<NeighborBlockInput> = emptyList(),
    @SerialName("amv_job_links") val amvJobLinks: List<AmvJobLinkInput> = emptyList()
)

@Serializable
data class JobInput(val id: String, val duration: Double, @SerialName("due_date") val dueDate: Double)

@Serializable
data class TbmInput(val id: String, val start: Double, val end: Double)

@Serializable
data class ArhInput(
    val id: String,
    @SerialName("avail_start") val availStart: Double,
    @SerialName("avail_end") val availEnd: Double,
    @SerialName("dur_min") val durMin: Double,
    @SerialName("dur_prob") val durProb: Double,
    @SerialName("dur_max") val durMax: Double
)

@Serializable
data class NeighborBlockInput(
    val id: String, val type: String = "PRODUCTION",
    val start: Double, val end: Double,
    @SerialName("due_date") val dueDate: Double = 0.0
)

@Serializable
data class JobLinkInput(
    @SerialName("job_id") val jobId: String,
    @SerialName("ready_time") val readyTime: Double
)

@Serializable
data class AmvJobLinkInput(
    @SerialName("job_id") val jobId: String,
    @SerialName("expected_start_on_next") val expectedStartOnNext: Double
)

// ─── UI-only state helpers ─────────────────────────────────────────────────

data class ArhUiState(
    val id: String, val availStart: Double, val availEnd: Double,
    val durMin: Double, val durProb: Double, val durMax: Double
)

data class NeighborBlockUi(val jobId: String, val start: Double, val end: Double)

data class AmaUiConfig(
    val id: String = "AMA_1",
    val blocks: List<NeighborBlockUi> = listOf(NeighborBlockUi("P1", 0.0, 50.0))
)

data class AmvUiConfig(
    val id: String = "AMV_1",
    val blocks: List<NeighborBlockUi> = listOf(NeighborBlockUi("P3", 70.0, 120.0))
)

enum class MachineStatus { STEADY, ANOMALY, RESCHEDULING, CONFLICT, RESOLVED, SHIFTED, DONE }

data class MultiMachineState(
    val currentStep: Int = 0,
    val amaId: String = "AMA_1",
    val amvId: String = "AMV_1",
    val amaSchedule: List<TaskBlock> = emptyList(),
    val amsSchedule: List<TaskBlock> = emptyList(),
    val amvSchedule: List<TaskBlock> = emptyList(),
    val amvOriginalSchedule: List<TaskBlock> = emptyList(),
    val messages: List<NegotiationMessage> = emptyList(),
    val logs: List<LogEvent> = emptyList(),
    val anomalyTime: Double = 8.0,
    val amaDeliveryTime: Double = 45.0,
    val amvExpectedTime: Double = 85.0,
    val amaStatus: MachineStatus = MachineStatus.STEADY,
    val amsStatus: MachineStatus = MachineStatus.STEADY,
    val amvStatus: MachineStatus = MachineStatus.STEADY,
    val masOutput: MASOutput? = null,
    val upstreamConflict: Boolean = false,
    val downstreamConflict: Boolean = false
)