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

    std::optional<CBMProposal> propose(
        double startTime,
        const std::vector<ProductionJob>& jobs,
        const std::vector<TBMBlock>& tbmBlocks) const
    {
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

            CBMProposal bestProp;
            double bestTardiness = std::numeric_limits<double>::max();

            // Try all permutations of production jobs
            auto perm = jobs;
            std::sort(perm.begin(), perm.end(),
                [](const auto& a, const auto& b){ return a.id < b.id; });

            do {
                // For each permutation, try all CBM insertion positions
                for (int pos = 0; pos <= static_cast<int>(perm.size()); ++pos) {
                    auto schedule = Scheduler::buildSchedule(
                        startTime, perm, pos,
                        window.start, repairDuration, tbmBlocks);

                    // Check CBM actually fits in the window
                    bool cbmValid = true;
                    for (const auto& b : schedule) {
                        if (b.type == "CBM" && b.end.prob > window.end + 0.001) {
                            cbmValid = false;
                            break;
                        }
                    }
                    if (!cbmValid) continue;

                    double tardiness = 0;
                    for (const auto& b : schedule) {
                        if (b.type == "PRODUCTION")
                            tardiness += std::max(0.0, b.end.prob - b.dueDate);
                    }

                    if (tardiness < bestTardiness) {
                        bestTardiness = tardiness;
                        bestProp.arhId = id;
                        bestProp.cbmStart = window.start;
                        bestProp.cbmDuration = repairDuration;
                        bestProp.schedule = schedule;
                    }
                }
            } while (std::next_permutation(perm.begin(), perm.end(),
                [](const auto& a, const auto& b){ return a.id < b.id; }));

            if (bestTardiness < std::numeric_limits<double>::max()) {
                std::cout << "[" << id << "]   Best tardiness=" << bestTardiness << "\n";
                std::cout << "[" << id << "]   Schedule:\n";
                for (const auto& b : bestProp.schedule) {
                    std::cout << "[" << id << "]     " << b.id
                              << " [" << b.start << " -> " << b.end << "]";
                    if (b.type == "PRODUCTION") std::cout << " due=" << b.dueDate;
                    std::cout << " (" << b.type << ")\n";
                }
                std::cout << "------------------------------------------------------------\n";
                return bestProp;
            }
        }

        std::cout << "[" << id << "]   REJECTED.\n";
        std::cout << "------------------------------------------------------------\n";
        return std::nullopt;
    }
};
