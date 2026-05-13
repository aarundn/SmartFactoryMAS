#pragma once
#include "DataStructures.h"
#include "JsonLogger.h"
#include <string>
#include <vector>
#include <algorithm>

class ARH {
public:
    std::string id;
    std::vector<TimeInterval> availabilities;
    FuzzyNumber repairDuration;
    std::vector<std::string> competencies;

    // الفني يراعي وقت العطل (alertTime) لكي لا يقترح السفر عبر الزمن
    std::vector<CBMProposal> propose(double alertTime) const {
        std::vector<CBMProposal> proposals;
        jsonLog(id, "Received CFP. Repair Duration: " + repairDuration.str());

        for (const auto& window : availabilities) {
            double actualStart = std::max(window.start, alertTime);
            double expectedFinish = actualStart + repairDuration.prob;

            if (expectedFinish <= window.end) {
                CBMProposal prop;
                prop.arhId = id;
                prop.cbmStart = actualStart;
                prop.cbmDuration = repairDuration;
                proposals.push_back(prop);
                jsonLog(id, "  Proposed valid window starting at " + std::to_string((int)actualStart));
            } else {
                jsonLog(id, "  Window [" + std::to_string((int)window.start) + ", " + std::to_string((int)window.end) + "] too small or in the past. Skipping.", "warn");
            }
        }
        return proposals;
    }
};