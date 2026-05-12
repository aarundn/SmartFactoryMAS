package com.example.smartfactorymas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── JSON output models from C++ MAS engine ───────────────────────────────────

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
    val schedule: List<ScheduleBlockDto>
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
    @SerialName("alert_time") val alertTime: Double = 8.0,
    @SerialName("scheduling_start") val schedulingStart: Double = 24.0,
    val w1: Double = 0.75,
    val w2: Double = 0.25,
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

// ─── UI State ──────────────────────────────────────────────────────────────────

data class ArhUiState(
    val id: String = "ARH_1",
    val availStart: Double = 24.0,
    val availEnd: Double = 50.0,
    val durMin: Double = 4.0,
    val durProb: Double = 7.0,
    val durMax: Double = 9.0
)
