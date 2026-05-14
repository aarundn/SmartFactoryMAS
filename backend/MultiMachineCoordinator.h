#pragma once
#include "AMA.h"
#include "AMV.h"
#include "JsonLogger.h"
#include <vector>
#include <sstream>
#include <iostream>

class MultiMachineCoordinator {
public:
    MultiMachineResult negotiate(std::vector<ScheduleBlock> amsSched, AMA& ama, AMV& amv) {
        MultiMachineResult r;
        r.amsSchedule = amsSched;

        for (size_t i = 0; i < r.amsSchedule.size(); ++i) {
            auto& b = r.amsSchedule[i];
            if (b.type != "PRODUCTION") continue;

            // ── 1. التفاوض القبلي (UPSTREAM - AMA) ─────────────────────
            double ready = ama.getReadyTime(b.id);
            if (ready > 0 && b.start.prob < ready) {
                r.upstreamConflict = true;
                bool ok = ama.handleMMessage(b.id, b.start.prob);
                r.messages.push_back({"M_MESSAGE", "AMS", ama.id, b.id, ready, b.start.prob, ok, ""});

                if (!ok) {
                    double delay = ready - b.start.prob;

                    // 🌟 الإصلاح السحري: إضافة التأخير لجميع القيم صراحةً لتجاوز خطأ الجمع في C++ 🌟
                    b.start.min += delay;
                    b.start.prob += delay;
                    b.start.max += delay;

                    b.end.min += delay;
                    b.end.prob += delay;
                    b.end.max += delay;

                    // إزاحة المهام اللاحقة (Cascade Shift) لمنع التداخل
                    for (size_t j = i + 1; j < r.amsSchedule.size(); ++j) {
                        if (r.amsSchedule[j].start.prob < r.amsSchedule[j-1].end.prob) {
                            double overlap = r.amsSchedule[j-1].end.prob - r.amsSchedule[j].start.prob;
                            r.amsSchedule[j].start.min += overlap;
                            r.amsSchedule[j].start.prob += overlap;
                            r.amsSchedule[j].start.max += overlap;

                            r.amsSchedule[j].end.min += overlap;
                            r.amsSchedule[j].end.prob += overlap;
                            r.amsSchedule[j].end.max += overlap;
                        }
                    }
                }
            }

            // ── 2. التفاوض البعدي (DOWNSTREAM - AMV) ───────────────────
            double exp = amv.getExpectedArrival(b.id);
            if (exp > 0 && b.end.prob > exp) {
                r.downstreamConflict = true;
                double delay = b.end.prob - exp;

                amv.handleIMessage(b.id, b.end.prob);
                r.messages.push_back({"I_MESSAGE", "AMS", amv.id, b.id, exp, b.end.prob, true, ""});
            }
        }

        r.amaSchedule = ama.schedule;
        r.amvSchedule = amv.schedule;
        emitJson(r, ama.id, amv.id);
        return r;
    }

private:
    void emitJson(const MultiMachineResult& r, const std::string& amaId, const std::string& amvId) {
        std::ostringstream j;
        j << "{\"type\":\"multi_machine_result\""
          << ",\"ama_id\":\"" << amaId << "\""
          << ",\"amv_id\":\"" << amvId << "\""
          << ",\"upstream_conflict\":"   << (r.upstreamConflict   ? "true" : "false")
          << ",\"downstream_conflict\":" << (r.downstreamConflict ? "true" : "false");

        j << ",\"ams_schedule\": [";
        for (size_t i = 0; i < r.amsSchedule.size(); ++i) {
            if (i > 0) j << ",";
            auto& b = r.amsSchedule[i];
            j << "{\"id\":\"" << b.id << "\",\"type\":\"" << b.type << "\""
              << ",\"start_min\":" << b.start.min << ",\"start_prob\":" << b.start.prob << ",\"start_max\":" << b.start.max
              << ",\"end_min\":" << b.end.min << ",\"end_prob\":" << b.end.prob << ",\"end_max\":" << b.end.max << "}";
        }
        j << "]";

        j << ",\"ama_schedule\": [";
        for (size_t i = 0; i < r.amaSchedule.size(); ++i) {
            if (i > 0) j << ",";
            auto& b = r.amaSchedule[i];
            j << "{\"id\":\"" << b.id << "\",\"type\":\"" << b.type << "\""
              << ",\"start_min\":" << b.start.min << ",\"start_prob\":" << b.start.prob << ",\"start_max\":" << b.start.max
              << ",\"end_min\":" << b.end.min << ",\"end_prob\":" << b.end.prob << ",\"end_max\":" << b.end.max << "}";
        }
        j << "]";

        j << ",\"amv_schedule\": [";
        for (size_t i = 0; i < r.amvSchedule.size(); ++i) {
            if (i > 0) j << ",";
            auto& b = r.amvSchedule[i];
            j << "{\"id\":\"" << b.id << "\",\"type\":\"" << b.type << "\""
              << ",\"start_min\":" << b.start.min << ",\"start_prob\":" << b.start.prob << ",\"start_max\":" << b.start.max
              << ",\"end_min\":" << b.end.min << ",\"end_prob\":" << b.end.prob << ",\"end_max\":" << b.end.max << "}";
        }
        j << "]";

        j << ",\"messages\":[";
        for (size_t i = 0; i < r.messages.size(); ++i) {
            const auto& m = r.messages[i];
            if (i > 0) j << ",";
            j << "{\"type\":\""           << m.type          << "\""
              << ",\"from\":\""           << m.from          << "\""
              << ",\"to\":\""             << m.to            << "\""
              << ",\"job_id\":\""         << m.jobId         << "\""
              << ",\"original_time\":"    << m.originalTime
              << ",\"requested_time\":"   << m.requestedTime
              << ",\"accepted\":"         << (m.accepted ? "true" : "false")
              << "}";
        }
        j << "]}";
        std::cout << j.str() << std::endl;
    }
};