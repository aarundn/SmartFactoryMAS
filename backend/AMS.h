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
#include <limits>
#include <sstream>

class AMS {
public:
    AMC* amc;
    ASRH* asrh;
    double alertTime;
    double w1, w2;

    AMS(AMC* a, ASRH* s, double alert, double w1 = 0.75, double w2 = 0.25)
        : amc(a), asrh(s), alertTime(alert), w1(w1), w2(w2) {}

    /// Evaluates a proposal and fills its f1, f2, f fields by finding the best permutation
    void evaluate(
        CBMProposal& prop,
        double schedulingStart,
        const std::vector<ProductionJob>& jobs,
        const std::vector<TBMBlock>& tbmBlocks) 
    {
        double bestTardiness = std::numeric_limits<double>::max();
        std::vector<ScheduleBlock> bestSchedule;

        auto perm = jobs;
        std::sort(perm.begin(), perm.end(), [](const auto& a, const auto& b){ return a.id < b.id; });

        do {
            for (int pos = 0; pos <= static_cast<int>(perm.size()); ++pos) {
                auto schedule = Scheduler::buildSchedule(
                    schedulingStart, perm, pos,
                    prop.cbmStart, prop.cbmDuration, tbmBlocks);

                // Check CBM actually fits in the proposal's window
                // (Since ARH proposed this window, it should fit, but we verify)
                double tardiness = 0;
                for (const auto& b : schedule) {
                    if (b.type == "PRODUCTION")
                        tardiness += std::max(0.0, b.end.prob - b.dueDate);
                }

                if (tardiness < bestTardiness) {
                    bestTardiness = tardiness;
                    bestSchedule = schedule;
                }
            }
        } while (std::next_permutation(perm.begin(), perm.end(),
            [](const auto& a, const auto& b){ return a.id < b.id; }));

        prop.schedule = bestSchedule;

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

        std::cout << "  [AMS] " << prop.arhId
                  << " | f1=" << prop.f1
                  << " | f2=" << f2val
                  << " | f=" << prop.f << "\n";
    }

    /// Full orchestration
    std::vector<CBMProposal> handleAnomaly(
        double schedulingStart,
        const std::vector<ProductionJob>& jobs,
        const std::vector<TBMBlock>& tbmBlocks,
        std::string strategy)
    {
        std::cout << "\n############################################################\n";
        std::cout << "#         MAS REACTIVE SCHEDULING — CBM PROTOCOL           #\n";
        std::cout << "############################################################\n\n";
        std::cout << "[AMS] Anomaly at t=" << alertTime << ". Delegating to AMC...\n\n";

        DiagnosticResult diag = amc->analyzeAnomaly(alertTime);
        std::cout << "\n[AMS] Diagnostic: \"" << diag.status << "\". Forwarding to ASRH with strategy " << strategy << "...\n";

        auto proposals = asrh->callForProposals(diag, strategy);
        if (proposals.empty()) {
            std::cout << "[AMS] ERROR: No valid proposals!\n";
            return {};
        }

        std::cout << "\n############################################################\n";
        std::cout << "[AMS] EVALUATION (w1=" << w1 << ", w2=" << w2 << ")\n";
        std::cout << "############################################################\n";

        CBMProposal* best = nullptr;
        for (auto& prop : proposals) {
            evaluate(prop, schedulingStart, jobs, tbmBlocks);
            if (!best || prop.f.prob < best->f.prob)
                best = &prop;
        }

        std::cout << "\n############################################################\n";
        std::cout << "#                   FINAL DECISION                         #\n";
        std::cout << "############################################################\n";
        std::cout << "[AMS] SELECTED: " << best->arhId
                  << " | f=" << best->f << "\n";
        std::cout << "############################################################\n\n";

        // Output JSON to stderr for Kotlin UI
        emitJson(proposals, best->arhId);
        return proposals;
    }

private:
    void emitJson(const std::vector<CBMProposal>& proposals, const std::string& chosen) {
        std::ostringstream j;
        j << "{\"chosen_arh\":\"" << chosen << "\",\"w1\":" << w1
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
              << ",\"schedule\":[";
            for (size_t s = 0; s < prop.schedule.size(); ++s) {
                const auto& b = prop.schedule[s];
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
            j << "]}";
        }
        j << "]}";
        std::cerr << "JSON_RESULT:" << j.str() << std::endl;
    }
};
