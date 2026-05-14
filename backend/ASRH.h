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

    // ── Helper: slide a start time forward until it clears all TBM blocks ───
    static double resolveStart(
            double rawStart,
            const FuzzyNumber& duration,
            const std::vector<TBMBlock>& tbmBlocks)
    {
        // Sort TBMs by start (defensive copy)
        auto sorted = tbmBlocks;
        std::sort(sorted.begin(), sorted.end(),
                [](const TBMBlock& a, const TBMBlock& b){ return a.start < b.start; });

        double current = rawStart;
        bool pushed = true;
        while (pushed) {
            pushed = false;
            for (const auto& tbm : sorted) {
                double cbmEnd = current + duration.prob;
                // Overlap condition: CBM starts before TBM ends AND CBM ends after TBM starts
                if (current < tbm.end && cbmEnd > tbm.start) {
                    current = tbm.end;   // push CBM to start right after this TBM
                    pushed = true;       // re-check all blocks from new position
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
            const std::vector<TBMBlock>& tbmBlocks)   // ← added
    {
        std::string skill = diag.requiredCompetence.empty() ? "Mechanical" : diag.requiredCompetence;
        jsonLog("ASRH", "Triggered: CBM required. Required skill: " + skill + ".", "info", 1);
        jsonLog("ASRH", "Strategy selected: " + strategy + ". Scanning technicians...", "info", 2);

        // ── STEP 3: Pre-filter by competence + window viability ──────────────
        std::vector<ARH> qualifiedARHs;
        for (const auto& arh : registeredARHs) {
            bool hasCompetence = false;
            for (const auto& comp : arh.competencies) {
                if (comp == skill) { hasCompetence = true; break; }
            }
            if (!hasCompetence) {
                jsonLog("ASRH", "SKIPPED " + arh.id + ": missing skill '" + skill + "'.", "warn", 3);
                continue;
            }

            // Basic window viability (ignoring TBMs for now — refined below)
            bool hasViableWindow = false;
            for (const auto& window : arh.availabilities) {
                double raw = std::max(window.start, alertTime);
                if (raw < window.end) { hasViableWindow = true; break; }
            }
            if (!hasViableWindow) {
                jsonLog("ASRH", "SKIPPED " + arh.id
                                + ": shift expired before alertTime=" + std::to_string((int)alertTime) + ".",
                        "warn", 3);
                continue;
            }
            qualifiedARHs.push_back(arh);
        }

        jsonLog("ASRH", "Found " + std::to_string(qualifiedARHs.size())
                + " competent technician(s). Sending CFP...", "info", 4);

        // ── STEPS 4 & 5: CFP + Reception + TBM-aware adjustment ─────────────
        std::vector<CBMProposal> proposals;
        std::string respondingARHs;

        for (const auto& arh : qualifiedARHs) {
            auto props = arh.propose(alertTime);

            for (auto& prop : props) {
                // Slide cbmStart forward past any overlapping TBM block
                double adjustedStart = resolveStart(prop.cbmStart, prop.cbmDuration, tbmBlocks);

                if (adjustedStart != prop.cbmStart) {
                    jsonLog("ASRH",
                            arh.id + ": cbmStart pushed from t="
                                    + std::to_string((int)prop.cbmStart)
                                    + " to t=" + std::to_string((int)adjustedStart)
                                    + " (TBM conflict resolved).", "info", 5);
                }

                // Re-validate: adjusted start must still fit inside the ARH's shift
                double adjustedFinish = adjustedStart + prop.cbmDuration.prob;
                bool fitsInShift = false;
                for (const auto& window : arh.availabilities) {
                    if (adjustedStart >= window.start && adjustedFinish <= window.end) {
                        fitsInShift = true;
                        break;
                    }
                }

                if (!fitsInShift) {
                    jsonLog("ASRH",
                            "REJECTED " + arh.id
                                    + ": adjusted finish t=" + std::to_string((int)adjustedFinish)
                                    + " exceeds shift end (TBM pushed CBM out of window).", "warn", 5);
                    continue;
                }

                prop.cbmStart = adjustedStart;  // commit the adjusted value
                proposals.push_back(prop);
                if (!respondingARHs.empty()) respondingARHs += " and ";
                respondingARHs += arh.id;
                jsonLog("ASRH",
                        "Accepted " + arh.id + ": CBM at t=" + std::to_string((int)adjustedStart)
                                + ", dur=(" + prop.cbmDuration.str() + ").", "info", 5);
            }
        }

        // ── STEP 6: Forward ──────────────────────────────────────────────────
        if (respondingARHs.empty())
            jsonLog("ASRH", "No valid proposals received.", "error", 6);
        else
            jsonLog("ASRH", "Forwarding proposals from " + respondingARHs + " to AMS.", "info", 6);

        return proposals;
    }

    void confirm(const std::string& arhId) {
        jsonLog("ASRH", "Confirmation sent to " + arhId + ". Rejecting other proposals.", "info", 7);
    }
};