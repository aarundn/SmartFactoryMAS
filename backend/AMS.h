#pragma once
#include "AMC.h"
#include "ASRH.h"
#include "AMA.h"
#include "AMV.h"
#include "NeighborMachine.h"
#include "MultiMachineCoordinator.h"
#include "DataStructures.h"
#include "Scheduler.h"
#include "JsonLogger.h"
#include <limits>
#include <sstream>
#include <algorithm>
#include <map>

class AMS {
public:
    AMC* amc;
    ASRH* asrh;
    double alertTime, w1, w2;

    AMA* ama = nullptr;
    AMV* amv = nullptr;

    AMS(AMC* a, ASRH* s, double alert, double w1, double w2)
            : amc(a), asrh(s), alertTime(alert), w1(w1), w2(w2) {}

    void setNeighbors(AMA* upstream, AMV* downstream) {
        ama = upstream;
        amv = downstream;
    }

    std::vector<CBMProposal> handleAnomaly(
            double schedulingStart,
            const std::vector<ProductionJob>& jobs,
            const std::vector<TBMBlock>& tbmBlocks,
            std::string strategy,
            double rulMin, double rulProb, double rulMax,
            std::string mode = "single")
    {
        jsonLog("AMS", "=== Phase 1: Single-Machine Scheduling ===");
        jsonLog("AMS", "Anomaly at t=" + std::to_string((int)alertTime) + " | Mode: " + mode);

        DiagnosticResult diag = amc->analyzeAnomaly(alertTime, rulMin, rulProb, rulMax);
        auto allProposals = asrh->callForProposals(diag, strategy, alertTime, schedulingStart, tbmBlocks);

        if (allProposals.empty()) {
            jsonLog("AMS", "ERROR: No valid proposals. System Abort.", "error");
            emitResultJson({}, "NONE");
            return {};
        }

        // Evaluate all Early/Late proposals
        for (auto& prop : allProposals) {
            evaluate(prop, schedulingStart, jobs, tbmBlocks);
        }

        // 🌟 FIX 2: Filter to keep only the BEST proposal per Technician 🌟
        std::map<std::string, CBMProposal> bestPerARH;
        for (const auto& prop : allProposals) {
            if (bestPerARH.find(prop.arhId) == bestPerARH.end() || prop.f.prob < bestPerARH[prop.arhId].f.prob) {
                bestPerARH[prop.arhId] = prop;
            }
        }

        std::vector<CBMProposal> finalProposals;
        CBMProposal* best = nullptr;

        for (auto& pair : bestPerARH) {
            finalProposals.push_back(pair.second);
        }

        for (auto& prop : finalProposals) {
            if (!best || prop.f.prob < best->f.prob) best = &prop;
        }

        jsonLog("AMS", "SELECTED: " + best->arhId + " | f=" + best->f.str());
        asrh->confirm(best->arhId);
        emitResultJson(finalProposals, best->arhId); // Emit only the clean, filtered list

        if (mode == "multi") {
            if (ama != nullptr && amv != nullptr) {
                jsonLog("AMS", "=== Phase 2: Multi-Machine Negotiation ===");
                MultiMachineCoordinator coordinator;
                coordinator.negotiate(best->schedule, *ama, *amv);
            } else {
                jsonLog("AMS", "Multi-machine mode requested but no neighbors configured.", "warn");
            }
        }

        return finalProposals;
    }

private:
    void evaluate(
            CBMProposal& prop,
            double schedulingStart,
            const std::vector<ProductionJob>& jobs,
            const std::vector<TBMBlock>& tbmBlocks)
    {
        prop.tracks.push_back(Scheduler::buildSchedule(schedulingStart, jobs, prop.cbmStart, prop.cbmDuration, tbmBlocks));

        double bestTardiness = std::numeric_limits<double>::max();
        std::vector<ScheduleBlock> bestSchedule;
        auto perm = jobs;

        if (jobs.size() <= 8) {
            std::sort(perm.begin(), perm.end(), [](const auto& a, const auto& b){ return a.id < b.id; });
            do {
                auto schedule = Scheduler::buildSchedule(schedulingStart, perm, prop.cbmStart, prop.cbmDuration, tbmBlocks);
                double tardiness = 0;
                for (const auto& b : schedule) {
                    if (b.type == "PRODUCTION") tardiness += std::max(0.0, b.end.prob - b.dueDate);
                }
                if (tardiness < bestTardiness) {
                    bestTardiness = tardiness;
                    bestSchedule  = schedule;
                }
            } while (std::next_permutation(perm.begin(), perm.end(), [](const auto& a, const auto& b){ return a.id < b.id; }));

        } else {
            std::sort(perm.begin(), perm.end(), [](const auto& a, const auto& b){ return a.dueDate < b.dueDate; });
            bestSchedule = Scheduler::buildSchedule(schedulingStart, perm, prop.cbmStart, prop.cbmDuration, tbmBlocks);
        }

        prop.schedule = bestSchedule;
        prop.tracks.push_back(bestSchedule);

        FuzzyNumber sumDelay(0,0,0);
        int jobCount = 0;
        for (const auto& block : prop.schedule) {
            if (block.type == "PRODUCTION") {
                sumDelay = sumDelay + (block.end - block.dueDate).maxOfZero();
                jobCount++;
            }
        }

        prop.f1 = (jobCount > 0) ? sumDelay / jobCount : FuzzyNumber(0,0,0);
        double f2val = std::max(0.0, prop.cbmStart - alertTime);
        prop.f2 = FuzzyNumber(f2val, f2val, f2val);

        // 🌟 FIX 1: Economic Multiplier to balance the scales! 🌟
        double economicProductionMultiplier = 4.0;
        prop.f  = (prop.f1 * (w1 * economicProductionMultiplier)) + (prop.f2 * w2);

        jsonLog("AMS", prop.arhId + " | f=" + prop.f.str());
    }

    std::string buildScheduleJson(const std::vector<ScheduleBlock>& sched) const {
        std::ostringstream j;
        j << "[";
        for (size_t s = 0; s < sched.size(); ++s) {
            const auto& b = sched[s];
            if (s > 0) j << ",";
            j << "{\"id\":\"" << b.id << "\",\"type\":\"" << b.type << "\""
              << ",\"start_min\":" << b.start.min << ",\"start_prob\":" << b.start.prob
              << ",\"start_max\":" << b.start.max << ",\"end_min\":" << b.end.min
              << ",\"end_prob\":" << b.end.prob << ",\"end_max\":" << b.end.max
              << ",\"due_date\":" << b.dueDate << "}";
        }
        j << "]";
        return j.str();
    }

    void emitResultJson(const std::vector<CBMProposal>& proposals, const std::string& chosen) {
        std::ostringstream j;
        j << "{\"type\":\"result\",\"chosen_arh\":\"" << chosen
          << "\",\"w1\":" << w1 << ",\"w2\":" << w2
          << ",\"alert_time\":" << alertTime << ",\"proposals\":[";

        for (size_t p = 0; p < proposals.size(); ++p) {
            const auto& prop = proposals[p];
            if (p > 0) j << ",";
            j << "{\"arh_id\":\"" << prop.arhId
              << "\",\"cbm_start\":" << prop.cbmStart
              << ",\"cbm_dur_min\":" << prop.cbmDuration.min
              << ",\"cbm_dur_prob\":" << prop.cbmDuration.prob
              << ",\"cbm_dur_max\":" << prop.cbmDuration.max
              << ",\"f1_min\":" << prop.f1.min << ",\"f1_prob\":" << prop.f1.prob
              << ",\"f1_max\":" << prop.f1.max << ",\"f2\":" << prop.f2.prob
              << ",\"f_min\":" << prop.f.min << ",\"f_prob\":" << prop.f.prob
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

        if (!g_silentMode) {
            std::cout << j.str() << std::endl;
        }
    }
};