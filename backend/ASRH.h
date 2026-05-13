/**
 * @file ASRH.h
 * @brief Agent Superviseur des Ressources Humaines — HR supervisor.
 */
#pragma once
#include "ARH.h"
#include <vector>
#include "JsonLogger.h"

class ASRH {
public:
    std::vector<ARH> registeredARHs;

    explicit ASRH(const std::vector<ARH>& arhs) : registeredARHs(arhs) {}

    std::vector<CBMProposal> callForProposals(
            DiagnosticResult diag,
            std::string strategy,
            double alertTime)
    {
        std::string skill = diag.requiredCompetence.empty() ? "Mechanical" : diag.requiredCompetence;
        jsonLog("ASRH", "Triggered: CBM required. Required skill: " + skill + ".", "info", 1);
        jsonLog("ASRH", "Strategy selected: " + strategy + ". Scanning technicians...", "info", 2);

        // Step 3: Pre-filtering (Matchmaking)
        // Step 3: Pre-filtering (Matchmaking)
        std::vector<ARH> qualifiedARHs;
        for (const auto& arh : registeredARHs) {
            // === NEW: Reject ARH whose entire availability window is already past ===
            bool hasValidWindow = false;
            for (const auto& window : arh.availabilities) {
                // Can this ARH even start? actualStart = max(window.start, alertTime)
                double actualStart = std::max(window.start, alertTime);
                double expectedFinish = actualStart + arh.repairDuration.prob;
                if (actualStart < window.end && expectedFinish <= window.end) {
                    hasValidWindow = true;
                    break;
                }
            }
            if (!hasValidWindow) {
                jsonLog("ASRH", "SKIPPED " + arh.id + ": No valid time window after alertTime="
                        + std::to_string((int)alertTime) + ".", "warn", 3);
                continue; // Don't even send CFP to this ARH
            }

            // Competence check (existing logic)
            bool hasCompetence = false;
            for (const auto& comp : arh.competencies) {
                if (comp == skill || diag.requiredCompetence.empty()) {
                    hasCompetence = true;
                    break;
                }
            }
            if (hasCompetence || diag.requiredCompetence.empty()) {
                qualifiedARHs.push_back(arh);
            }
        }

        jsonLog("ASRH", "Found " + std::to_string(qualifiedARHs.size()) + " qualified technicians. Sending CFP...", "info", 4);

        double deadlineLimit = 0.0;
        if (strategy == "SOM") {
            deadlineLimit = diag.estimatedRUL.min;
        } else if (strategy == "SOP") {
            deadlineLimit = diag.estimatedRUL.max;
        } else {
            deadlineLimit = diag.estimatedRUL.prob; // default
        }

        // Step 4 & 5: Reception
        std::vector<CBMProposal> proposals;
        std::string respondingARHs = "";

        for (const auto& arh : qualifiedARHs) {
            // نمرر وقت العطل للفني لكي يبني عرضه بشكل صحيح
            auto props = arh.propose(alertTime);

            for (auto& prop : props) {
                double expectedFinish = prop.cbmStart + prop.cbmDuration.prob;

                // نتحقق من الموعد النهائي للآلة (RUL)
                if (expectedFinish <= deadlineLimit) {
                    proposals.push_back(prop);
                    if (!respondingARHs.empty()) respondingARHs += " and ";
                    respondingARHs += arh.id;
                } else {
                    jsonLog("ASRH", "REJECTED " + arh.id + ": Expected finish (" + std::to_string((int)expectedFinish) + ") exceeds RUL deadline limit (" + std::to_string((int)deadlineLimit) + ").", "warn", 5);
                }
            }
        }

        // Step 6: Forwarding
        if (respondingARHs.empty()) {
            jsonLog("ASRH", "No valid proposals received.", "error", 6);
        } else {
            jsonLog("ASRH", "Received proposals from " + respondingARHs + ". Forwarding to AMS.", "info", 6);
        }

        return proposals;
    }

    void confirm(const std::string& arhId) {
        jsonLog("ASRH", "Confirmation sent to " + arhId + ". Rejecting other proposals.", "info", 7);
    }
};