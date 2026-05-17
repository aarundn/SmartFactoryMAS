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

        double actualShift = 0.0;

        // 🌟 1. التحقق من القدرة على امتصاص التأخير (السر وراء الـ Stable) 🌟
        for (auto& block : schedule) {
            if (block.id == jobId && block.type == "PRODUCTION") {
                // إذا كان وقت بدء المهمة المبرمج أصلاً متأخراً عن وقت الوصول الجديد
                if (block.start.prob >= newArrival) {
                    return 0.0; // تم امتصاص الصدمة بنجاح! لا يوجد تأثير على المصنع
                } else {
                    // التأخير تجاوز الفراغ، الآلة مضطرة لإزاحة جدولها
                    actualShift = newArrival - block.start.prob;
                    break;
                }
            }
        }

        // 🌟 2. تطبيق الإزاحة الذكية فقط إذا فشل الامتصاص 🌟
        if (actualShift > 0) {
            double cursor = newArrival;

            for (auto& block : schedule) {
                // نتجاهل الصيانة ونتجاهل المهام التي انتهت قبل العطل
                if (block.type != "PRODUCTION" || block.start.prob < originalArrival) continue;

                double duration = block.end.prob - block.start.prob;
                bool overlap;

                // القفز فوق مهام الصيانة (TBM/CBM) لتجنب التداخل الكارثي
                do {
                    overlap = false;
                    for (const auto& fixedBlock : schedule) {
                        if (fixedBlock.type == "PRODUCTION") continue;

                        double fs = fixedBlock.start.prob;
                        double fe = fixedBlock.end.prob;

                        if (cursor < fe && (cursor + duration) > fs) {
                            cursor = fe; // القفز إلى ما بعد انتهاء الصيانة
                            overlap = true;
                        }
                    }
                } while (overlap);

                // تحديث الأوقات بالمؤشر الجديد الآمن
                block.start = FuzzyNumber(cursor, cursor, cursor);
                block.end   = FuzzyNumber(cursor + duration, cursor + duration, cursor + duration);
                expectedArrivalTimes[block.id] = cursor;

                // تحريك المؤشر للمهمة التالية
                cursor += duration;
            }
        }

        return actualShift; // نُرجع قيمة الإزاحة الفعلية (أو 0.0 إذا تم الامتصاص)
    }
};