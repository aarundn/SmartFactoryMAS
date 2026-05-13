package com.example.smartfactorymas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── JSONL event models from C++ MAS engine ───────────────────────────────────

@Serializable
sealed class EngineEvent {
    abstract val type: String
}

@Serializable
@SerialName("log")
data class LogEvent(
    override val type: String = "log",
    val agent: String,
    val msg: String,
    val level: String = "info",
    val step: Int? = null
) : EngineEvent()

@Serializable
@SerialName("proposal")
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

@Serializable
@SerialName("result")
data class ResultEvent(
    override val type: String = "result",
    @SerialName("chosen_arh") val chosenArh: String,
    val w1: Double,
    val w2: Double,
    @SerialName("alert_time") val alertTime: Double,
    val proposals: List<ProposalDto>
) : EngineEvent()

@Serializable
data class ScheduleBlockDto(
    val id: String,
    val type: String,
    @SerialName("start_min") val startMin: Double,
    @SerialName("start_prob") val startProb: Double,
    @SerialName("start_max") val startMax: Double,
    @SerialName("end_min") val endMin: Double,
    @SerialName("end_prob") val endProb: Double,
    @SerialName("end_max") val endMax: Double,
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
    val w1: Double,
    val w2: Double,
    @SerialName("alert_time") val alertTime: Double,
    val proposals: List<ProposalDto>
) {
    fun chosenProposal(): ProposalDto? = proposals.find { it.arhId == chosenArh }
}

// ─── Input model sent to the C++ engine (optional dynamic config) ─────────────
@Serializable
data class EngineInput(
    val strategy: String = "SOM",
    @SerialName("alert_time") val alertTime: Double = 8.0,
    @SerialName("scheduling_start") val schedulingStart: Double = 24.0,
    val w1: Double = 0.75,
    val w2: Double = 0.25,
    @SerialName("rul_min") val rulMin: Double = 100.0,
    @SerialName("rul_prob") val rulProb: Double = 120.0,
    @SerialName("rul_max") val rulMax: Double = 140.0,
    val jobs: List<JobInput> = emptyList(),
    @SerialName("tbm_blocks") val tbmBlocks: List<TbmInput> = emptyList(),
    @SerialName("arh_agents") val arhAgents: List<ArhInput> = emptyList()
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

data class ArhUiState(
    val id: String, val availStart: Double, val availEnd: Double,
    val durMin: Double, val durProb: Double, val durMax: Double
)
