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

    // ── Helper: slide cbmStart forward past any overlapping TBM block ────────
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
                // Overlap: CBM starts before TBM ends AND CBM ends after TBM starts
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

        // Strategy defines the HARD deadline:
        //   SOM → must finish before risk zone starts (RUL.min)
        //   SOP → must finish before machine fails    (RUL.max)
        double deadlineLimit = (strategy == "SOM")
                ? diag.estimatedRUL.min + (diag.estimatedRUL.prob - diag.estimatedRUL.min)
                : diag.estimatedRUL.max;

        jsonLog("ASRH", "Triggered: CBM required. Skill: " + skill + ".", "info", 1);
        jsonLog("ASRH",
                "Strategy: " + strategy
                        + " | Deadline: t=" + std::to_string((int)deadlineLimit)
                        + " | RUL.max: t=" + std::to_string((int)diag.estimatedRUL.max)
                        + " | Machine free at: t=" + std::to_string((int)schedulingStart),
                "info", 2);

        // ── STEP 3: Pre-filter by competence ────────────────────────────────
        std::vector<ARH> qualifiedARHs;
        for (const auto& arh : registeredARHs) {
            bool hasCompetence = false;
            for (const auto& comp : arh.competencies)
                if (comp == skill) { hasCompetence = true; break; }

            if (!hasCompetence) {
                jsonLog("ASRH", "SKIPPED " + arh.id + ": missing skill '" + skill + "'.", "warn", 3);
                continue;
            }
            qualifiedARHs.push_back(arh);
        }

        jsonLog("ASRH", "Found " + std::to_string(qualifiedARHs.size()) + " competent technician(s). Sending CFP...", "info", 4);

        // ── STEPS 4 & 5: CFP + Reception with all 5 constraints ─────────────
        std::vector<CBMProposal> proposals;
        std::string respondingARHs;

        for (const auto& arh : qualifiedARHs) {
            for (const auto& window : arh.availabilities) {

                // C1 + C2: earliest the ARH can physically start
                double rawStart = std::max({ window.start, alertTime, schedulingStart });

                if (rawStart >= window.end) {
                    continue; // Shift expired
                }

                // Calculate the "Early" option
                double earlyStart = resolveStart(rawStart, arh.repairDuration, tbmBlocks);
                double earlyFinish = earlyStart + arh.repairDuration.prob;

                // C4 & C5 Constraints
                if (earlyFinish > window.end || earlyStart >= diag.estimatedRUL.max || earlyFinish > deadlineLimit) {
                    continue; // Invalid
                }

                // 🌟 SOP FIX: Calculate the "Late" option 🌟
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
                prop.arhId       = arh.id;
                prop.cbmDuration = arh.repairDuration;

                // 🌟 THE STRATEGY FILTER 🌟
                if (strategy == "SOP" && lateIsSafe && (lateStart >= earlyStart + 1.0)) {
                    // SOP selected: Push maintenance to the last safe minute
                    prop.cbmStart = lateStart;
                    jsonLog("ASRH", "SOP Strategy: Selected late start (t=" + std::to_string((int)lateStart) + ") for " + arh.id, "info");
                } else {
                    // SOM selected (Or SOP fallback if late is unsafe): Fix it ASAP
                    prop.cbmStart = earlyStart;
                    jsonLog("ASRH", strategy + " Strategy: Selected early start (t=" + std::to_string((int)earlyStart) + ") for " + arh.id, "info");
                }

                proposals.push_back(prop);
                if (!respondingARHs.empty()) respondingARHs += " and ";
                respondingARHs += arh.id;
            }
        }

        // ── STEP 6: Forward to AMS ───────────────────────────────────────────
        if (respondingARHs.empty())
            jsonLog("ASRH", "No valid proposals within " + strategy + " deadline (t=" + std::to_string((int)deadlineLimit) + ").", "error", 6);
        else
            jsonLog("ASRH", "Forwarding proposals from " + respondingARHs + " to AMS.", "info", 6);

        return proposals;
    }

    void confirm(const std::string& arhId) {
        jsonLog("ASRH",
                "Confirmation sent to " + arhId + ". Rejecting other proposals.",
                "info", 7);
    }
};