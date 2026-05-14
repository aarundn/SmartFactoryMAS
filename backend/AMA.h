#pragma once
#include <string>
#include <vector>
#include <map>
#include "DataStructures.h"
#include "JsonLogger.h"

class AMA {
public:
    std::string id;
    std::vector<ScheduleBlock> schedule;
    std::map<std::string, double> jobCompletionTimes;

    AMA() = default;
    AMA(const std::string& id, const std::vector<ScheduleBlock>& schedule)
            : id(id), schedule(schedule)
    {
        for (const auto& block : schedule)
            if (block.type == "PRODUCTION")
                jobCompletionTimes[block.id] = block.end.prob;
    }

    double getReadyTime(const std::string& jobId) const {
        auto it = jobCompletionTimes.find(jobId);
        return (it != jobCompletionTimes.end()) ? it->second : -1.0;
    }

    bool handleMMessage(const std::string& jobId, double requestedTime) {
        jsonLog(id, "Received M_Message for job " + jobId
                + ": Can you finish at t=" + std::to_string((int)requestedTime)
                + " instead of t=" + std::to_string((int)getReadyTime(jobId)) + "?");

        ScheduleBlock* target = nullptr;
        for (auto& b : schedule)
            if (b.id == jobId && b.type == "PRODUCTION") { target = &b; break; }

        if (!target) {
            jsonLog(id, "REJECTED: job " + jobId + " not found.", "warn");
            return false;
        }

        double duration = target->end.prob - target->start.prob;
        double newStart = requestedTime - duration;

        // 🌟 التعديل الجديد: منع الآلة من بدء العمل في وقت سلبي
        if (newStart < 0) {
            jsonLog(id, "REJECTED: shifting would result in negative start time (" + std::to_string((int)newStart) + ").", "warn");
            return false;
        }

        for (const auto& other : schedule) {
            if (other.id == jobId) continue;
            if (other.start.prob < requestedTime && other.end.prob > newStart) {
                jsonLog(id, "REJECTED: shifting " + jobId + " conflicts with " + other.id + ".", "warn");
                return false;
            }
        }

        target->start = FuzzyNumber(newStart, newStart, newStart);
        target->end   = FuzzyNumber(requestedTime, requestedTime, requestedTime);
        jobCompletionTimes[jobId] = requestedTime;

        jsonLog(id, "ACCEPTED: job " + jobId + " rescheduled to finish at t=" + std::to_string((int)requestedTime) + ".");
        return true;
    }
};