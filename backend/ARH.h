/**
 * @file ARH.h
 * @brief Agent Ressource Humaine (ARH) — Maintenance engineer.
 */
#pragma once
#include "DataStructures.h"
#include "JsonLogger.h"
#include <string>
#include <vector>
#include <algorithm> // من أجل std::max

class ARH {
public:
    std::string id;
    std::vector<TimeInterval> availabilities;
    FuzzyNumber repairDuration;
    std::vector<std::string> competencies;

    // تم إضافة alertTime لكي يعرف الفني متى تعطلت الآلة بالضبط
    std::vector<CBMProposal> propose(double alertTime) const
    {
        std::vector<CBMProposal> proposals;
        jsonLog(id, "Received CFP. Duration: " + repairDuration.str());

        for (const auto& window : availabilities) {

            // الحساب الذكي: الفني لا يمكنه البدء قبل أن تتعطل الآلة
            // سيبدأ إما عند تعطل الآلة، أو عند بداية مناوبته (أيهما متأخر أكثر)
            double actualStart = std::max(window.start, alertTime);

            // متى سينتهي من الإصلاح؟
            double expectedFinish = actualStart + repairDuration.prob;

            // هل هذا الإصلاح يناسب نافذة عمله؟ (يجب أن ينتهي قبل أن يغادر المصنع)
            if (expectedFinish <= window.end) {
                CBMProposal prop;
                prop.arhId = id;
                prop.cbmStart = actualStart;
                prop.cbmDuration = repairDuration;
                proposals.push_back(prop);
                jsonLog(id, "  Proposed valid window starting at " + std::to_string((int)actualStart));
            } else {
                jsonLog(id, "  Window [" + std::to_string((int)window.start) + ", " + std::to_string((int)window.end) + "] too small or in the past after alertTime=" + std::to_string((int)alertTime) + ". Skipping.", "warn");
            }
        }
        return proposals;
    }
};