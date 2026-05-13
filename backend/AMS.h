/**
 * @file AMS.h
 * @brief Agent Machine Sujet (AMS) — Central decision-maker.
 *
 * Evaluates proposals using:
 *   f1 = avg fuzzy production delay = Σ maxOfZero(end_i - due_i) / n
 *   f2 = maintenance delay = max(0, cbmStart - alertTime)
 *   f  = w1*f1 + w2*f2
 */
#pragma once
#include "AMC.h"
#include "ASRH.h"
#include "DataStructures.h"
#include "Scheduler.h"
#include <limits>
#include <sstream>
#include <algorithm>
#include "JsonLogger.h"

class AMS {
public:
    AMC* amc;
    ASRH* asrh;
    double alertTime;
    double w1, w2;

    AMS(AMC* a, ASRH* s, double alert, double w1 = 0.75, double w2 = 0.25)
            : amc(a), asrh(s), alertTime(alert), w1(w1), w2(w2) {}

    /// Helper function to serialize a vector of ScheduleBlocks into a JSON array string
    std::string buildScheduleJson(const std::vector<ScheduleBlock>& sched) const {
        std::ostringstream j;
        j << "[";
        for (size_t s = 0; s < sched.size(); ++s) {
            const auto& b = sched[s];
            if (s > 0) j << ",";
            j << "{\"id\":\"" << b.id << "\""
              << ",\"type\":\"" << b.type << "\""
              << ",\"start_min\":" << b.start.min
              << ",\"start_prob\":" << b.start.prob
              << ",\"start_max\":" << b.start.max
              << ",\"end_min\":" << b.end.min
              << ",\"end_prob\":" << b.end.prob
              << ",\"end_max\":" << b.end.max
              << ",\"due_date\":" << b.dueDate << "}";
        }
        j << "]";
        return j.str();
    }

    /// Evaluates a proposal and emits a "proposal" event
    void evaluate(
            CBMProposal& prop,
            double schedulingStart,
            const std::vector<ProductionJob>& jobs,
            const std::vector<TBMBlock>& tbmBlocks)
    {
        // --- STEP 1 - SAVE THE NAIVE TRACK ---
        // 'jobs' is passed in its original chronological order
        auto naiveSchedule = Scheduler::buildSchedule(
                schedulingStart, jobs, prop.cbmStart, prop.cbmDuration, tbmBlocks);
        prop.tracks.push_back(naiveSchedule); // Save as Track 0 (Naive)

        // --- STEP 2 - THE PERMUTATION OPTIMIZER ---
        double bestTardiness = std::numeric_limits<double>::max();
        std::vector<ScheduleBlock> bestSchedule;

        auto perm = jobs;
        std::sort(perm.begin(), perm.end(), [](const auto& a, const auto& b){ return a.id < b.id; });

        do {
            auto schedule = Scheduler::buildSchedule(
                    schedulingStart, perm, prop.cbmStart, prop.cbmDuration, tbmBlocks);

            double tardiness = 0;
            for (const auto& b : schedule) {
                if (b.type == "PRODUCTION")
                    tardiness += std::max(0.0, b.end.prob - b.dueDate);
            }

            if (tardiness < bestTardiness) {
                bestTardiness = tardiness;
                bestSchedule = schedule;
            }
        } while (std::next_permutation(perm.begin(), perm.end(),
                [](const auto& a, const auto& b){ return a.id < b.id; }));

        prop.schedule = bestSchedule;
        prop.tracks.push_back(bestSchedule); // Save as Track 1 (Optimal Result)

        // --- STEP 3 - CALCULATE OBJECTIVES ---
        FuzzyNumber sumDelay(0,0,0);
        int jobCount = 0;
        for (const auto& block : prop.schedule) {
            if (block.type == "PRODUCTION") {
                FuzzyNumber delay = (block.end - block.dueDate).maxOfZero();
                sumDelay = sumDelay + delay;
                jobCount++;
            }
        }
        prop.f1 = (jobCount > 0) ? sumDelay / jobCount : FuzzyNumber(0,0,0);

        double f2val = std::max(0.0, prop.cbmStart - alertTime);
        prop.f2 = FuzzyNumber(f2val, f2val, f2val);

        prop.f = (prop.f1 * w1) + (prop.f2 * w2);

        jsonLog("AMS", prop.arhId + " | f1=" + prop.f1.str() + " | f2=" + std::to_string((int)f2val) + " | f=" + prop.f.str());

        // --- STEP 4 - EMIT JSON EVENT WITH TRACKS ---
        std::ostringstream j;
        j << "{\"type\":\"proposal\",\"arh_id\":\"" << prop.arhId << "\""
          << ",\"cbm_start\":" << prop.cbmStart
          << ",\"cbm_dur_min\":" << prop.cbmDuration.min
          << ",\"cbm_dur_prob\":" << prop.cbmDuration.prob
          << ",\"cbm_dur_max\":" << prop.cbmDuration.max
          << ",\"f1_min\":" << prop.f1.min
          << ",\"f1_prob\":" << prop.f1.prob
          << ",\"f1_max\":" << prop.f1.max
          << ",\"f2\":" << prop.f2.prob
          << ",\"f_min\":" << prop.f.min
          << ",\"f_prob\":" << prop.f.prob
          << ",\"f_max\":" << prop.f.max
          << ",\"schedule\":" << buildScheduleJson(prop.schedule)
          << ",\"tracks\":[";

        for (size_t t = 0; t < prop.tracks.size(); ++t) {
            if (t > 0) j << ",";
            j << buildScheduleJson(prop.tracks[t]);
        }
        j << "]}";

        jsonEmit(j.str());
    }

    /// Full orchestration
    std::vector<CBMProposal> handleAnomaly(
            double schedulingStart,
            const std::vector<ProductionJob>& jobs,
            const std::vector<TBMBlock>& tbmBlocks,
            std::string strategy)
    {
        jsonLog("AMS", "Anomaly at t=" + std::to_string((int)alertTime) + ". Delegating to AMC...");

        DiagnosticResult diag = amc->analyzeAnomaly(alertTime);
        jsonLog("AMS", "Diagnostic: \"" + diag.status + "\". Forwarding to ASRH with strategy " + strategy + "...");

        // FIX: Added 'alertTime' to properly enforce the timeline in ASRH.h
        auto proposals = asrh->callForProposals(diag, strategy, alertTime);

        if (proposals.empty()) {
            jsonLog("AMS", "ERROR: No valid proposals!", "error");
            emitResultJson({}, "NONE");
            return {};
        }

        jsonLog("AMS", "EVALUATION (w1=" + std::to_string(w1) + ", w2=" + std::to_string(w2) + ")");

        CBMProposal* best = nullptr;
        for (auto& prop : proposals) {
            evaluate(prop, schedulingStart, jobs, tbmBlocks);
            if (!best || prop.f.prob < best->f.prob)
                best = &prop;
        }

        jsonLog("AMS", "SELECTED: " + best->arhId + " | f=" + best->f.str());
        asrh->confirm(best->arhId);

        // Output final result event
        emitResultJson(proposals, best->arhId);
        return proposals;
    }

private:
    void emitResultJson(const std::vector<CBMProposal>& proposals, const std::string& chosen) {
        std::ostringstream j;
        // FIX: Re-added 'JSON_RESULT:' so Kotlin's BufferedReader finds the line successfully
        j << "JSON_RESULT:{\"type\":\"result\",\"chosen_arh\":\"" << chosen << "\",\"w1\":" << w1
          << ",\"w2\":" << w2 << ",\"alert_time\":" << alertTime
          << ",\"proposals\":[";

        for (size_t p = 0; p < proposals.size(); ++p) {
            const auto& prop = proposals[p];
            if (p > 0) j << ",";
            j << "{\"arh_id\":\"" << prop.arhId << "\""
              << ",\"cbm_start\":" << prop.cbmStart
              << ",\"cbm_dur_min\":" << prop.cbmDuration.min
              << ",\"cbm_dur_prob\":" << prop.cbmDuration.prob
              << ",\"cbm_dur_max\":" << prop.cbmDuration.max
              << ",\"f1_min\":" << prop.f1.min
              << ",\"f1_prob\":" << prop.f1.prob
              << ",\"f1_max\":" << prop.f1.max
              << ",\"f2\":" << prop.f2.prob
              << ",\"f_min\":" << prop.f.min
              << ",\"f_prob\":" << prop.f.prob
              << ",\"f_max\":" << prop.f.max
              << ",\"schedule\":" << buildScheduleJson(prop.schedule)
              << ",\"tracks\":[";

            for (size_t t = 0; t < prop.tracks.size(); ++t) {
                if (t > 0) j << ",";
                j << buildScheduleJson(prop.tracks[t]);
            }
            j << "]}";
        }
        j << "]}";

        // Output directly using std::cout to prevent Kotlin crashes
        std::cout << j.str() << std::endl;
    }
};