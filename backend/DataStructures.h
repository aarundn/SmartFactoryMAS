#pragma once
#include <string>
#include <vector>
#include "FuzzyNumber.h"
#include <map>

// ─── Split timing metrics (one anomaly position: Début / Milieu / Fin) ───────
struct SplitMetrics {
    double single_ms = 0.0;   // avg single-machine resolution time (ms)
    double multi_ms  = 0.0;   // avg multi-machine resolution time  (ms)
};

// ─── Stability for ONE resolution level ─────────────────────────────────────
//   Stable      : finalDelay ≈ initialDelay   (disruption absorbed)
//   Improved    : finalDelay < initialDelay   (re-ordering helped)
//   Deteriorated: finalDelay > initialDelay   (CBM forced cascade)
struct StabilityMetrics {
    double stable_percent       = 0.0;
    double improved_percent     = 0.0;
    double deteriorated_percent = 0.0;
};

// ─── Both resolution levels (for Tableau 4.7) ────────────────────────────────
struct BatchStabilityIndex {
    StabilityMetrics single_machine;   // "Le retard une-machine"
    StabilityMetrics multi_machine;    // "Le retard multi-machines"
};

// ─── Batch run parameters ────────────────────────────────────────────────────
struct BatchParams {
    int    num_machines;
    int    num_jobs;
    int    num_arhs;
    double w1;           // production weight  (SOP → w1 > w2)
    double w2;           // maintenance weight (SOM → w2 > w1)
    int    num_scenarios;
};

// ─── One point on the reactivity chart ──────────────────────────────────────
struct BatchReactivityChartData {
    int    num_jobs;
    double single_machine_time_ms;
    double multi_machine_time_ms;
};

// ─── Complete result from BatchSimulator::runBatch() ────────────────────────
struct BatchSimulationResult {
    BatchStabilityIndex                   stability_index;
    std::string                           ai_recommendation;
    std::vector<BatchReactivityChartData> reactivity_chart_data;
    std::string                           csv_export_data;

    // Per-position averages for Tableau 4.6
    SplitMetrics debut_splits;
    SplitMetrics milieu_splits;
    SplitMetrics fin_splits;
};

// ─── Scheduling primitives ───────────────────────────────────────────────────
struct TimeInterval {
    double start;
    double end;
};

struct DiagnosticResult {
    std::string status;
    FuzzyNumber estimatedRUL;
    std::string requiredCompetence;
};

struct ProductionJob {
    std::string id;
    double      duration;
    double      dueDate;
};

struct TBMBlock {
    std::string id;
    double      start;
    double      end;
};

struct ScheduleBlock {
    std::string id;
    std::string type;      // "PRODUCTION" | "CBM" | "TBM"
    FuzzyNumber start;
    FuzzyNumber end;
    double      dueDate = 0.0;
};

struct CBMProposal {
    std::string  arhId;
    double       cbmStart;
    FuzzyNumber  cbmDuration;

    std::vector<ScheduleBlock>              schedule;
    std::vector<std::vector<ScheduleBlock>> tracks;

    FuzzyNumber f1;   // avg tardiness per job (the delay metric)
    FuzzyNumber f2;   // response time (cbmStart − alertTime)
    FuzzyNumber f;    // weighted: f1·w1 + f2·w2
};

// ─── Multi-machine negotiation ───────────────────────────────────────────────
struct ReactivityData {
    int    num_jobs;
    double single_machine_time_ms;
    double multi_machine_time_ms;
};