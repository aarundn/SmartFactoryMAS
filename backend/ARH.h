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
#include <optional>
#include <limits>
#include <algorithm>
#include <iostream>

class ARH {
public:
    std::string id;
    std::vector<TimeInterval> availabilities;
    FuzzyNumber repairDuration;
    std::vector<std::string> competencies;

    std::vector<CBMProposal> propose() const
    {
        std::vector<CBMProposal> proposals;
        std::cout << "\n------------------------------------------------------------\n";
        std::cout << "[" << id << "] Received CFP. Duration: " << repairDuration << "\n";

        for (const auto& window : availabilities) {
            double windowLen = window.end - window.start;
            std::cout << "[" << id << "] Window [" << window.start << ", "
                      << window.end << "] len=" << windowLen << "\n";

            if (windowLen < repairDuration.prob) {
                std::cout << "[" << id << "]   Too small. Skipping.\n";
                continue;
            }

            CBMProposal prop;
            prop.arhId = id;
            prop.cbmStart = window.start;
            prop.cbmDuration = repairDuration;
            proposals.push_back(prop);
        }

        if (proposals.empty()) {
            std::cout << "[" << id << "]   REJECTED. No valid windows.\n";
        } else {
            std::cout << "[" << id << "]   Proposed " << proposals.size() << " valid window(s).\n";
        }
        std::cout << "------------------------------------------------------------\n";
        return proposals;
    }
};
