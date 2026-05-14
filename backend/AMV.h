#pragma once
#include <string>
#include <vector>
#include <map>
#include "DataStructures.h"
#include "JsonLogger.h"

class AMV {
public:
    std::string id;
    std::vector<ScheduleBlock> schedule;
    std::map<std::string, double> expectedArrivalTimes;

    AMV() = default;
    AMV(const std::string& id, const std::vector<ScheduleBlock>& schedule)
            : id(id), schedule(schedule)
    {
        for (const auto& block : schedule)
            if (block.type == "PRODUCTION")
                expectedArrivalTimes[block.id] = block.start.prob;
    }

    double getExpectedArrival(const std::string& jobId) const {
        auto it = expectedArrivalTimes.find(jobId);
        return (it != expectedArrivalTimes.end()) ? it->second : -1.0;
    }

    double handleIMessage(const std::string& jobId, double newArrival) {
        double originalArrival = getExpectedArrival(jobId);
        if (originalArrival < 0) return 0.0;

        double delay = newArrival - originalArrival;
        if (delay <= 0) return 0.0;

        for (auto& block : schedule) {
            if (block.start.prob >= originalArrival) {
                // 🌟 إضافة التأخير صراحةً لجميع المتغيرات 🌟
                block.start.min += delay; block.start.prob += delay; block.start.max += delay;
                block.end.min   += delay; block.end.prob   += delay; block.end.max   += delay;
                expectedArrivalTimes[block.id] = block.start.prob;
            }
        }
        return delay;
    }
};