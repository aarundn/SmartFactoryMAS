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
                ? diag.estimatedRUL.min
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
                jsonLog("ASRH",
                        "SKIPPED " + arh.id + ": missing skill '" + skill + "'.",
                        "warn", 3);
                continue;
            }
            qualifiedARHs.push_back(arh);
        }

        jsonLog("ASRH",
                "Found " + std::to_string(qualifiedARHs.size())
                        + " competent technician(s). Sending CFP...", "info", 4);

        // ── STEPS 4 & 5: CFP + Reception with all 5 constraints ─────────────
        //
        //  C1. cbmStart >= alertTime       (anomaly must exist first)
        //  C2. cbmStart >= schedulingStart (cannot interrupt running job)
        //  C3. cbmStart clears all TBMs    (no overlap with fixed maintenance)
        //  C4. cbmFinish <= window.end     (must complete within ARH shift)
        //  C5a. cbmStart < RUL.max         (machine not yet failed)
        //  C5b. cbmFinish <= deadlineLimit (within strategy-defined window)
        //
        std::vector<CBMProposal> proposals;
        std::string respondingARHs;

        for (const auto& arh : qualifiedARHs) {
            for (const auto& window : arh.availabilities) {

                // C1 + C2: earliest the ARH can physically start
                double rawStart = std::max({ window.start, alertTime, schedulingStart });

                // Quick pre-check: shift already expired before we can even begin
                if (rawStart >= window.end) {
                    jsonLog("ASRH",
                            "SKIPPED " + arh.id
                                    + ": shift ends t=" + std::to_string((int)window.end)
                                    + " but earliest start is t=" + std::to_string((int)rawStart)
                                    + " (shift expired).",
                            "warn", 5);
                    continue;
                }

                // C3: push past any overlapping TBM block
                double adjustedStart = resolveStart(rawStart, arh.repairDuration, tbmBlocks);

                if (adjustedStart != rawStart) {
                    jsonLog("ASRH",
                            arh.id + ": cbmStart pushed t="
                                    + std::to_string((int)rawStart)
                                    + " → t=" + std::to_string((int)adjustedStart)
                                    + " (TBM conflict resolved).",
                            "info", 5);
                }

                double adjustedFinish = adjustedStart + arh.repairDuration.prob;

                // C4: must complete within ARH's own shift
                if (adjustedFinish > window.end) {
                    jsonLog("ASRH",
                            "REJECTED " + arh.id
                                    + ": finish t=" + std::to_string((int)adjustedFinish)
                                    + " exceeds shift end t=" + std::to_string((int)window.end)
                                    + " (cannot complete within shift).",
                            "warn", 5);
                    continue;
                }

                // C5a: machine must not have already failed
                if (adjustedStart >= diag.estimatedRUL.max) {
                    jsonLog("ASRH",
                            "REJECTED " + arh.id
                                    + ": cbmStart t=" + std::to_string((int)adjustedStart)
                                    + " is AFTER machine failure (RUL.max="
                                    + std::to_string((int)diag.estimatedRUL.max) + ").",
                            "error", 5);
                    continue;
                }

                // C5b: must finish within the strategy deadline
                if (adjustedFinish > deadlineLimit) {
                    jsonLog("ASRH",
                            "REJECTED " + arh.id
                                    + ": finish t=" + std::to_string((int)adjustedFinish)
                                    + " exceeds " + strategy + " deadline t="
                                    + std::to_string((int)deadlineLimit) + ".",
                            "warn", 5);
                    continue;
                }

                // All constraints satisfied — accept proposal
                CBMProposal prop;
                prop.arhId       = arh.id;
                prop.cbmStart    = adjustedStart;
                prop.cbmDuration = arh.repairDuration;
                proposals.push_back(prop);
                if (!respondingARHs.empty()) respondingARHs += " and ";
                respondingARHs += arh.id;

                jsonLog("ASRH",
                        "ACCEPTED " + arh.id
                                + ": CBM at t=" + std::to_string((int)adjustedStart)
                                + ", finish t=" + std::to_string((int)adjustedFinish)
                                + " (deadline t=" + std::to_string((int)deadlineLimit) + ").",
                        "info", 5);
            }
        }

        // ── STEP 6: Forward to AMS ───────────────────────────────────────────
        if (respondingARHs.empty())
            jsonLog("ASRH",
                    "No valid proposals within " + strategy
                            + " deadline (t=" + std::to_string((int)deadlineLimit) + ").",
                    "error", 6);
        else
            jsonLog("ASRH",
                    "Forwarding proposals from " + respondingARHs + " to AMS.",
                    "info", 6);

        return proposals;
    }

    void confirm(const std::string& arhId) {
        jsonLog("ASRH",
                "Confirmation sent to " + arhId + ". Rejecting other proposals.",
                "info", 7);
    }
};