#pragma once
#include "ARH.h"
#include "DataStructures.h"
#include <vector>
#include <algorithm>
#include "JsonLogger.h"

class ASRH {
public:
    std::vector<ARH> registeredARHs;

    explicit ASRH(const std::vector<ARH>& arhs) : registeredARHs(arhs) {}

    static double resolveStart(
            double rawStart,
            const FuzzyNumber& duration,
            const std::vector<TBMBlock>& tbmBlocks)
    {
        auto sorted = tbmBlocks;
        std::sort(sorted.begin(), sorted.end(),
                [](const TBMBlock& a, const TBMBlock& b){ return a.start < b.start; });

        double current = rawStart;
        bool pushed = true;
        while (pushed) {
            pushed = false;
            for (const auto& tbm : sorted) {
                double cbmEnd = current + duration.prob;
                if (current < tbm.end && cbmEnd > tbm.start) {
                    current = tbm.end;
                    pushed = true;
                    break;
                }
            }
        }
        return current;
    }

    std::vector<CBMProposal> callForProposals(
            DiagnosticResult diag,
            std::string strategy,
            double alertTime,
            double schedulingStart,
            const std::vector<TBMBlock>& tbmBlocks)
    {
        std::string skill = diag.requiredCompetence.empty() ? "Mechanical" : diag.requiredCompetence;

        double deadlineLimit = (strategy == "SOM")
                ? diag.estimatedRUL.min + (diag.estimatedRUL.prob - diag.estimatedRUL.min)
                : diag.estimatedRUL.max;

        jsonLog("ASRH", "Triggered: CBM required. Skill: " + skill + ".", "info", 1);

        std::vector<ARH> qualifiedARHs;
        for (const auto& arh : registeredARHs) {
            bool hasCompetence = false;
            for (const auto& comp : arh.competencies)
                if (comp == skill) { hasCompetence = true; break; }
            if (hasCompetence) qualifiedARHs.push_back(arh);
        }

        std::vector<CBMProposal> proposals;
        std::string respondingARHs;

        for (const auto& arh : qualifiedARHs) {
            for (const auto& window : arh.availabilities) {

                double rawStart = std::max({ window.start, alertTime, schedulingStart });
                if (rawStart >= window.end) continue;

                double earlyStart = resolveStart(rawStart, arh.repairDuration, tbmBlocks);
                double earlyFinish = earlyStart + arh.repairDuration.prob;

                if (earlyFinish > window.end || earlyStart >= diag.estimatedRUL.max || earlyFinish > deadlineLimit) {
                    continue; // Invalid
                }

                // Calculate the "Late" option
                double lateStart = std::min(window.end, deadlineLimit) - arh.repairDuration.prob;
                bool lateIsSafe = true;

                // Ensure late start doesn't collide with a TBM block
                for (const auto& tbm : tbmBlocks) {
                    if (lateStart < tbm.end && (lateStart + arh.repairDuration.prob) > tbm.start) {
                        lateIsSafe = false;
                        break;
                    }
                }

                CBMProposal prop;
                prop.arhId       = arh.id; // Clean ID for the UI
                prop.cbmDuration = arh.repairDuration;

                // 🌟 THE STRATEGY FILTER 🌟
                if (strategy == "SOP" && lateIsSafe && (lateStart >= earlyStart + 1.0)) {
                    // SOP selected: Push maintenance to the last safe minute
                    prop.cbmStart = lateStart;
                    jsonLog("ASRH", "SOP Strategy: Selected late start for " + arh.id, "info");
                } else {
                    // SOM selected (Or SOP fallback if late is unsafe): Fix it ASAP
                    prop.cbmStart = earlyStart;
                    jsonLog("ASRH", "SOM Strategy (or SOP fallback): Selected early start for " + arh.id, "info");
                }

                proposals.push_back(prop);
                if (!respondingARHs.empty()) respondingARHs += " and ";
                respondingARHs += arh.id;
            }
        }

        if (respondingARHs.empty())
            jsonLog("ASRH", "No valid proposals within " + strategy + " deadline.", "error", 6);
        else
            jsonLog("ASRH", "Forwarding proposals from " + respondingARHs + " to AMS.", "info", 6);

        return proposals;
    }

    void confirm(const std::string& arhId) {
        jsonLog("ASRH", "Confirmation sent to " + arhId + ". Rejecting others.", "info", 7);
    }
};