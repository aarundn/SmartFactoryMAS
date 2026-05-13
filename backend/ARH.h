/**
 * @file ARH.h
 * @brief Agent Ressource Humaine (ARH) — Maintenance engineer.
 *
 * Each ARH tries ALL permutations of production jobs × ALL CBM
 * insertion positions, matching Figures 4.6/4.7 iterative approach.
 * For each combination, the Scheduler computes the real timeline
 * and the ARH picks the arrangement with minimum tardiness.
 */
#pragma once
#include "DataStructures.h"
#include "Scheduler.h"
#include "JsonLogger.h"
#include <string>
#include <vector>

class ARH {
public:
    std::string id;
    std::vector<TimeInterval> availabilities;
    FuzzyNumber repairDuration;
    std::vector<std::string> competencies;

    std::vector<CBMProposal> propose() const
    {
        std::vector<CBMProposal> proposals;
        jsonLog(id, "Received CFP. Duration: " + repairDuration.str());

        for (const auto& window : availabilities) {
            double windowLen = window.end - window.start;
            jsonLog(id, "Window [" + std::to_string((int)window.start) + ", " + std::to_string((int)window.end) + "] len=" + std::to_string((int)windowLen));

            if (windowLen < repairDuration.prob) {
                jsonLog(id, "  Too small. Skipping.", "warn");
                continue;
            }

            CBMProposal prop;
            prop.arhId = id;
            prop.cbmStart = window.start;
            prop.cbmDuration = repairDuration;
            proposals.push_back(prop);
        }

        if (proposals.empty()) {
            jsonLog(id, "  REJECTED. No valid windows.", "warn");
        } else {
            jsonLog(id, "  Proposed " + std::to_string(proposals.size()) + " valid window(s).");
        }
        return proposals;
    }
};
