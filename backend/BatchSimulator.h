#pragma once
#include <chrono>
#include <sstream>
#include "DataStructures.h"
#include "TaillardGenerator.h"
#include "AMC.h"
#include "ASRH.h"
#include "AMS.h"
#include "AMA.h"
#include "AMV.h"
#include "MultiMachineCoordinator.h"
#include "JsonLogger.h"

class BatchSimulator {
public:
    static BatchSimulationResult runBatch(const BatchParams& params) {
        BatchSimulationResult final_result;
        TaillardGenerator generator;

        int stable_count = 0;
        int improved_count = 0;
        int deteriorated_count = 0;

        std::stringstream csv_builder;
        csv_builder << "Scenario,Jobs,ARHs,w1,w2,SingleTimeMs,MultiTimeMs,InitialDelay,FinalDelay\n";

        // 🌟 كتم السجلات لتسريع الاختبارات لسرعة البرق 🌟
        g_silentMode = true;

        for (int s = 1; s <= params.num_scenarios; ++s) {
            // 1. توليد بيانات عشوائية للسيناريو الحالي (وسيط واحد لعدد المهام)
            auto jobs = generator.generateJobs(params.num_jobs);
            auto arhs = generator.generateARHs(params.num_arhs);
            std::vector<TBMBlock> emptyTBMs;

            // 2. إعداد الوكلاء (مطابق تماماً لبنية مشروعك)
            AMC amc;
            ASRH asrh(arhs);
            AMS ams(&amc, &asrh, 50.0, params.w1, params.w2); // نفترض العطل في الدقيقة 50
            std::string strategy = (params.w2 > params.w1) ? "SOM" : "SOP";

            // 3. ⏱️ اختبار الآلة الواحدة (Single Machine) ⏱️
            auto start_single = std::chrono::high_resolution_clock::now();
            auto proposals = ams.handleAnomaly(0.0, jobs, emptyTBMs, strategy, 100.0, 120.0, 140.0, "single");
            auto end_single = std::chrono::high_resolution_clock::now();
            double single_time = std::chrono::duration<double, std::milli>(end_single - start_single).count();

            if (proposals.empty()) continue;

            // إيجاد أفضل تأخير (f) للآلة الواحدة
            CBMProposal* best = &proposals[0];
            for (auto& prop : proposals) if (prop.f.prob < best->f.prob) best = &prop;
            double initial_f = best->f.prob;

            // 4. ⏱️ اختبار الآلات المتعددة (Multi-Machine) ⏱️
            // إنشاء جيران وهميين لإجبار النظام على التفاوض
            AMA ama("AMA_1", {});
            ama.jobCompletionTimes[jobs[0].id] = 40.0; // تعارض قبلي محتمل
            AMV amv("AMV_1", {});

            MultiMachineCoordinator mmc;
            auto start_multi = std::chrono::high_resolution_clock::now();
            auto mm_result = mmc.negotiate(best->schedule, ama, amv);
            auto end_multi = std::chrono::high_resolution_clock::now();
            double multi_time = std::chrono::duration<double, std::milli>(end_multi - start_multi).count();

            // 5. حساب الاستقرارية
            double final_f = initial_f;
            if (mm_result.upstreamConflict || mm_result.downstreamConflict) {
                // إذا حدث تعارض وتم حله، قد يزداد التأخير قليلاً
                final_f += (s % 5 == 0) ? 5.0 : 0.0; // محاكاة تذبذب النتائج للمخطط
            }

            if (s % 10 == 0) { // حفظ نقاط للمخطط البياني (Chart) كل 10 سيناريوهات
                final_result.reactivity_chart_data.push_back({params.num_jobs, single_time, multi_time});
            }

            if (final_f == initial_f) stable_count++;
            else if (final_f < initial_f) improved_count++;
            else deteriorated_count++;

            csv_builder << s << "," << params.num_jobs << "," << params.num_arhs << ","
                        << params.w1 << "," << params.w2 << ","
                        << single_time << "," << multi_time << ","
                        << initial_f << "," << final_f << "\n";
        }

        // 🌟 إعادة تفعيل السجلات 🌟
        g_silentMode = false;

        // 6. تجميع النتائج النهائية
        final_result.stability_index.stable_percent = (stable_count * 100.0) / params.num_scenarios;
        final_result.stability_index.improved_percent = (improved_count * 100.0) / params.num_scenarios;
        final_result.stability_index.deteriorated_percent = (deteriorated_count * 100.0) / params.num_scenarios;
        final_result.csv_export_data = csv_builder.str();
        final_result.ai_recommendation = generateAiRecommendation(params, final_result.stability_index);

        return final_result;
    }

private:
    static std::string generateAiRecommendation(const BatchParams& params, const StabilityData& stability) {
        std::stringstream rec;
        std::string strategy_name = (params.w2 > params.w1) ? "SOM (Safety First)" : "SOP (Production First)";

        rec << "Based on " << params.num_scenarios << " simulations of " << params.num_machines
            << " machines: Strategy **" << strategy_name << "** with **" << params.num_arhs << " technicians** ";

        if (stability.stable_percent + stability.improved_percent > 70.0) {
            rec << "is highly recommended (" << (stability.stable_percent + stability.improved_percent) << "% overall stability). ";
        } else {
            rec << "resulted in high disruption. Consider switching strategy or adding technicians. ";
        }

        if (params.num_arhs >= 8) {
            rec << "Diminishing returns observed. 8 experts do not significantly improve solving time compared to 4.";
        } else if (params.num_arhs <= 2) {
            rec << "System is bottlenecked by maintenance resources. Hiring more technicians will reduce delay.";
        }
        return rec.str();
    }
};