#pragma once
#include "AMA.h"
#include "AMV.h"
#include "JsonLogger.h"
#include <vector>
#include <sstream>
#include <iostream>
#include <algorithm>

class MultiMachineCoordinator {
public:
    MultiMachineResult negotiate(std::vector<ScheduleBlock> amsSched, AMA& ama, AMV& amv) {
        MultiMachineResult r;
        r.amsSchedule = amsSched;

        // ── 1. التفاوض القبلي (UPSTREAM) وإعادة بناء الجدول ──
        for (size_t i = 0; i < r.amsSchedule.size(); ++i) {
            auto& b = r.amsSchedule[i];
            if (b.type != "PRODUCTION") continue;

            double ready = ama.getReadyTime(b.id);
            if (ready > 0 && b.start.prob < ready) {
                r.upstreamConflict = true;
                bool ok = ama.handleMMessage(b.id, b.start.prob);
                r.messages.push_back({"M_MESSAGE", "AMS", ama.id, b.id, ready, b.start.prob, ok, ""});

                if (!ok) {
                    jsonLog("MMC", "AMA REJECTED. Forcing AMS to shift jobs & jump over TBM/CBMs.", "warn");

                    // 🌟 خوارزمية التعبئة الذكية (Smart Repack) 🌟
                    for (size_t j = i; j < r.amsSchedule.size(); ++j) {
                        if (r.amsSchedule[j].type != "PRODUCTION") continue;

                        double earliestStart = r.amsSchedule[j].start.prob;

                        // المهمة المرفوضة تُجبر على البدء في وقت توفر القطعة
                        if (j == i) {
                            earliestStart = ready;
                        } else {
                            // المهام اللاحقة يجب أن تبدأ بعد انتهاء المهمة الإنتاجية السابقة
                            double prevProdEnd = 0;
                            for (int k = j - 1; k >= 0; --k) {
                                if (r.amsSchedule[k].type == "PRODUCTION") {
                                    prevProdEnd = r.amsSchedule[k].end.prob;
                                    break;
                                }
                            }
                            earliestStart = std::max(earliestStart, prevProdEnd);
                        }

                        double duration = r.amsSchedule[j].end.prob - r.amsSchedule[j].start.prob;
                        bool overlap;

                        // 🌟 التحقق من الاصطدام مع الـ TBM و CBM والقفز فوقها 🌟
                        do {
                            overlap = false;
                            for (const auto& fixedBlock : r.amsSchedule) {
                                if (fixedBlock.type == "PRODUCTION") continue; // نتجاهل مهام الإنتاج

                                double fs = fixedBlock.start.prob;
                                double fe = fixedBlock.end.prob;

                                // إذا تداخلت المهمة مع الصيانة، اقفز لتبدأ بعد انتهاء الصيانة
                                if (earliestStart < fe && (earliestStart + duration) > fs) {
                                    earliestStart = fe;
                                    overlap = true;
                                }
                            }
                        } while (overlap);

                        // تعيين الأوقات الجديدة بشكل آمن
                        r.amsSchedule[j].start.min = earliestStart;
                        r.amsSchedule[j].start.prob = earliestStart;
                        r.amsSchedule[j].start.max = earliestStart;

                        r.amsSchedule[j].end.min = earliestStart + duration;
                        r.amsSchedule[j].end.prob = earliestStart + duration;
                        r.amsSchedule[j].end.max = earliestStart + duration;
                    }
                }
            }
        }

        // ── 2. ترتيب الجدول زمنياً قبل إرسال الرسائل البعدية ──
        std::sort(r.amsSchedule.begin(), r.amsSchedule.end(), [](const ScheduleBlock& a, const ScheduleBlock& b) {
            return a.start.prob < b.start.prob;
        });

        // ── 3. التفاوض البعدي (DOWNSTREAM - AMV) ──
        for (auto& b : r.amsSchedule) {
            if (b.type != "PRODUCTION") continue;

            double exp = amv.getExpectedArrival(b.id);
            if (exp > 0 && b.end.prob > exp) {
                r.downstreamConflict = true;
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
        j << "{\"type\":\"multi_machine_result\",\"ama_id\":\"" << amaId << "\",\"amv_id\":\"" << amvId << "\""
          << ",\"upstream_conflict\":" << (r.upstreamConflict ? "true" : "false")
          << ",\"downstream_conflict\":" << (r.downstreamConflict ? "true" : "false");

        auto printBlocks = [&j](const std::vector<ScheduleBlock>& sched) {
            j << "[";
            for (size_t i = 0; i < sched.size(); ++i) {
                if (i > 0) j << ",";
                auto& b = sched[i];
                j << "{\"id\":\"" << b.id << "\",\"type\":\"" << b.type << "\""
                  << ",\"start_min\":" << b.start.min << ",\"start_prob\":" << b.start.prob << ",\"start_max\":" << b.start.max
                  << ",\"end_min\":" << b.end.min << ",\"end_prob\":" << b.end.prob << ",\"end_max\":" << b.end.max << "}";
            }
            j << "]";
        };

        j << ",\"ams_schedule\":"; printBlocks(r.amsSchedule);
        j << ",\"ama_schedule\":"; printBlocks(r.amaSchedule);
        j << ",\"amv_schedule\":"; printBlocks(r.amvSchedule);

        j << ",\"messages\":[";
        for (size_t i = 0; i < r.messages.size(); ++i) {
            if (i > 0) j << ",";
            const auto& m = r.messages[i];
            j << "{\"type\":\"" << m.type << "\",\"from\":\"" << m.from << "\",\"to\":\"" << m.to << "\""
              << ",\"job_id\":\"" << m.jobId << "\",\"original_time\":" << m.originalTime
              << ",\"requested_time\":" << m.requestedTime << ",\"accepted\":" << (m.accepted ? "true" : "false") << "}";
        }
        j << "]}";
        std::cout << j.str() << std::endl;
    }
};