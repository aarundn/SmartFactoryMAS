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
        if (originalArrival < 0) {
            jsonLog(id, "I_Message ignored: job " + jobId + " not in schedule.", "warn");
            return 0.0;
        }

        double delay = newArrival - originalArrival;
        if (delay <= 0) {
            jsonLog(id, "I_Message for " + jobId + ": no delay. No shift needed.");
            return 0.0;
        }

        jsonLog(id, "Received I_Message: job " + jobId
                + " arrives at t=" + std::to_string((int)newArrival)
                + " instead of t=" + std::to_string((int)originalArrival)
                + ". Shifting +" + std::to_string((int)delay) + " units.");

        for (auto& block : schedule) {
            if (block.start.prob >= originalArrival) {
                block.start = block.start + delay;
                block.end   = block.end   + delay;
                expectedArrivalTimes[block.id] = block.start.prob;
                jsonLog(id, "  Shifted " + block.id
                        + " → [t=" + std::to_string((int)block.start.prob)
                        + ", t=" + std::to_string((int)block.end.prob) + "]");
            }
        }
        return delay;
    }
};