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
    double alertTime, w1, w2;

    AMS(AMC* a, ASRH* s, double alert, double w1, double w2) : amc(a), asrh(s), alertTime(alert), w1(w1), w2(w2) {}

    // هذه الدالة تطبع الجداول فقط، بدون أي عناوين!
    std::string buildScheduleJson(const std::vector<ScheduleBlock>& sched) const {
        std::ostringstream j;
        j << "[";
        for (size_t s = 0; s < sched.size(); ++s) {
            const auto& b = sched[s];
            if (s > 0) j << ",";
            j << "{\"id\":\"" << b.id << "\",\"type\":\"" << b.type << "\""
              << ",\"start_min\":" << b.start.min << ",\"start_prob\":" << b.start.prob << ",\"start_max\":" << b.start.max
              << ",\"end_min\":" << b.end.min << ",\"end_prob\":" << b.end.prob << ",\"end_max\":" << b.end.max
              << ",\"due_date\":" << b.dueDate << "}";
        }
        j << "]";
        return j.str();
    }

    void evaluate(CBMProposal& prop, double schedulingStart, const std::vector<ProductionJob>& jobs, const std::vector<TBMBlock>& tbmBlocks) {
        // 1. مسار الإزاحة لليمين (Track 0)
        prop.tracks.push_back(Scheduler::buildSchedule(schedulingStart, jobs, prop.cbmStart, prop.cbmDuration, tbmBlocks));

        double bestTardiness = std::numeric_limits<double>::max();
        std::vector<ScheduleBlock> bestSchedule;
        auto perm = jobs;
        std::sort(perm.begin(), perm.end(), [](const auto& a, const auto& b){ return a.id < b.id; });

        do {
            auto schedule = Scheduler::buildSchedule(schedulingStart, perm, prop.cbmStart, prop.cbmDuration, tbmBlocks);
            double tardiness = 0;
            for (const auto& b : schedule) {
                if (b.type == "PRODUCTION") tardiness += std::max(0.0, b.end.prob - b.dueDate);
            }
            if (tardiness < bestTardiness) {
                bestTardiness = tardiness;
                bestSchedule = schedule;
            }
        } while (std::next_permutation(perm.begin(), perm.end(), [](const auto& a, const auto& b){ return a.id < b.id; }));

        // 2. المسار النهائي (Track 1) والجدول النهائي
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
        prop.f = (prop.f1 * w1) + (prop.f2 * w2);

        jsonLog("AMS", prop.arhId + " | f=" + prop.f.str());
    }

    std::vector<CBMProposal> handleAnomaly(double schedulingStart, const std::vector<ProductionJob>& jobs, const std::vector<TBMBlock>& tbmBlocks, std::string strategy, double rulMin, double rulProb, double rulMax) {
        jsonLog("AMS", "Anomaly at t=" + std::to_string((int)alertTime) + ". Delegating to AMC...");
        DiagnosticResult diag = amc->analyzeAnomaly(alertTime, rulMin, rulProb, rulMax);
        auto proposals = asrh->callForProposals(diag, strategy, alertTime, schedulingStart, tbmBlocks);

        if (proposals.empty()) {
            jsonLog("AMS", "ERROR: No valid proposals! System Abort.", "error");
            emitResultJson({}, "NONE");
            return {};
        }

        CBMProposal* best = nullptr;
        for (auto& prop : proposals) {
            evaluate(prop, schedulingStart, jobs, tbmBlocks);
            if (!best || prop.f.prob < best->f.prob) best = &prop;
        }

        jsonLog("AMS", "SELECTED: " + best->arhId + " with score f=" + best->f.str());
        asrh->confirm(best->arhId);
        emitResultJson(proposals, best->arhId);
        return proposals;
    }

private:
    void emitResultJson(const std::vector<CBMProposal>& proposals, const std::string& chosen) {
        std::ostringstream j;
        // هنا يطبع العنوان مرة واحدة فقط في بداية النص
        j << "JSON_RESULT:{\"type\":\"result\",\"chosen_arh\":\"" << chosen << "\",\"w1\":" << w1
          << ",\"w2\":" << w2 << ",\"alert_time\":" << alertTime << ",\"proposals\":[";

        for (size_t p = 0; p < proposals.size(); ++p) {
            const auto& prop = proposals[p];
            if (p > 0) j << ",";
            j << "{\"arh_id\":\"" << prop.arhId << "\",\"cbm_start\":" << prop.cbmStart
              << ",\"cbm_dur_min\":" << prop.cbmDuration.min << ",\"cbm_dur_prob\":" << prop.cbmDuration.prob << ",\"cbm_dur_max\":" << prop.cbmDuration.max
              << ",\"f1_min\":" << prop.f1.min << ",\"f1_prob\":" << prop.f1.prob << ",\"f1_max\":" << prop.f1.max
              << ",\"f2\":" << prop.f2.prob << ",\"f_min\":" << prop.f.min << ",\"f_prob\":" << prop.f.prob << ",\"f_max\":" << prop.f.max
              << ",\"schedule\":" << buildScheduleJson(prop.schedule)
              << ",\"tracks\":[";
            for (size_t t = 0; t < prop.tracks.size(); ++t) {
                if (t > 0) j << ",";
                j << buildScheduleJson(prop.tracks[t]);
            }
            j << "]}";
        }
        j << "]}";
        std::cout << j.str() << std::endl;
    }
};