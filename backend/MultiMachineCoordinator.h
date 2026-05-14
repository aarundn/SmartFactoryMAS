#pragma once
#include "AMA.h"
#include "AMV.h"
#include "JsonLogger.h"
#include <vector>
#include <sstream>
#include <iostream>

class MultiMachineCoordinator {
public:

    MultiMachineResult negotiate(
            const std::vector<ScheduleBlock>& amsNewSchedule,
            AMA& ama,    // upstream — may modify its schedule
            AMV& amv)    // downstream — may shift its schedule
    {
        MultiMachineResult result;
        result.amsSchedule = amsNewSchedule;

        jsonLog("MMC", "=== Multi-Machine Negotiation Started ===");
        jsonLog("MMC", "Checking " + std::to_string(amsNewSchedule.size())
                + " blocks for upstream/downstream conflicts...");

        for (const auto& block : amsNewSchedule) {
            if (block.type != "PRODUCTION") continue;

            // ── UPSTREAM CONFLICT ────────────────────────────────────────────
            // AMS wants to start job before AMA has finished it
            double readyTime = ama.getReadyTime(block.id);
            if (readyTime > 0 && block.start.prob < readyTime) {
                result.upstreamConflict = true;
                jsonLog("MMC",
                        "UPSTREAM CONFLICT: " + block.id
                                + " — AMS start t=" + std::to_string((int)block.start.prob)
                                + " < AMA ready t=" + std::to_string((int)readyTime), "warn");

                // Delegate entirely to AMA — it decides and updates itself
                bool accepted = ama.handleMMessage(block.id, block.start.prob);

                NegotiationMessage msg;
                msg.type          = "M_MESSAGE";
                msg.from          = "AMS";
                msg.to            = ama.id;
                msg.jobId         = block.id;
                msg.originalTime  = readyTime;
                msg.requestedTime = block.start.prob;
                msg.accepted      = accepted;
                msg.reason        = accepted
                        ? "AMA accelerated delivery"
                        : "AMA cannot accelerate — AMS must respect t=" + std::to_string((int)readyTime);
                result.messages.push_back(msg);
            }

            // ── DOWNSTREAM CONFLICT ──────────────────────────────────────────
            // AMS finishes job later than AMV expected
            double expectedArrival = amv.getExpectedArrival(block.id);
            if (expectedArrival > 0 && block.end.prob > expectedArrival) {
                result.downstreamConflict = true;
                jsonLog("MMC",
                        "DOWNSTREAM CONFLICT: " + block.id
                                + " — AMS finish t=" + std::to_string((int)block.end.prob)
                                + " > AMV expected t=" + std::to_string((int)expectedArrival), "warn");

                // Delegate entirely to AMV — it always adapts
                double delay = amv.handleIMessage(block.id, block.end.prob);

                NegotiationMessage msg;
                msg.type          = "I_MESSAGE";
                msg.from          = "AMS";
                msg.to            = amv.id;
                msg.jobId         = block.id;
                msg.originalTime  = expectedArrival;
                msg.requestedTime = block.end.prob;
                msg.accepted      = true;
                msg.reason        = "AMV shifted " + std::to_string((int)delay) + " units right";
                result.messages.push_back(msg);
            }
        }

        // Collect final schedules from the agents themselves
        result.amaSchedule = ama.schedule;
        result.amvSchedule = amv.schedule;

        if (!result.upstreamConflict && !result.downstreamConflict)
            jsonLog("MMC", "No conflicts. All three machines are consistent.");

        jsonLog("MMC", "=== Negotiation Complete ===");
        emitJson(result, ama.id, amv.id);
        return result;
    }

private:
    std::string buildSchedJson(const std::vector<ScheduleBlock>& s) const {
        std::ostringstream j;
        j << "[";
        for (size_t i = 0; i < s.size(); ++i) {
            const auto& b = s[i];
            if (i > 0) j << ",";
            j << "{\"id\":\"" << b.id << "\",\"type\":\"" << b.type << "\""
              << ",\"start_min\":" << b.start.min << ",\"start_prob\":" << b.start.prob
              << ",\"start_max\":" << b.start.max << ",\"end_min\":" << b.end.min
              << ",\"end_prob\":" << b.end.prob << ",\"end_max\":" << b.end.max
              << ",\"due_date\":" << b.dueDate << "}";
        }
        j << "]";
        return j.str();
    }

    void emitJson(const MultiMachineResult& r,
            const std::string& amaId, const std::string& amvId)
    {
        std::ostringstream j;
        j << "{\"type\":\"multi_machine_result\""
          << ",\"upstream_conflict\":" << (r.upstreamConflict ? "true" : "false")
          << ",\"downstream_conflict\":" << (r.downstreamConflict ? "true" : "false")
          << ",\"ama_id\":\"" << amaId << "\",\"amv_id\":\"" << amvId << "\""
          << ",\"ama_schedule\":" << buildSchedJson(r.amaSchedule)
          << ",\"ams_schedule\":" << buildSchedJson(r.amsSchedule)
          << ",\"amv_schedule\":" << buildSchedJson(r.amvSchedule)
          << ",\"messages\":[";
        for (size_t i = 0; i < r.messages.size(); ++i) {
            const auto& m = r.messages[i];
            if (i > 0) j << ",";
            j << "{\"type\":\"" << m.type << "\",\"from\":\"" << m.from
              << "\",\"to\":\"" << m.to << "\",\"job_id\":\"" << m.jobId << "\""
              << ",\"original_time\":" << m.originalTime
              << ",\"requested_time\":" << m.requestedTime
              << ",\"accepted\":" << (m.accepted ? "true" : "false")
              << ",\"reason\":\"" << m.reason << "\"}";
        }
        j << "]}";
        std::cout << j.str() << std::endl;
    }
};