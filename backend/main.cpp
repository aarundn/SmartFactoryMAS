#include <iostream>
#include <vector>
#include <string>
#include <sstream>
#include <algorithm>
#include "FuzzyNumber.h"
#include "DataStructures.h"
#include "AMC.h"
#include "ARH.h"
#include "ASRH.h"
#include "AMA.h"
#include "AMV.h"
#include "AMS.h"
#include "JsonLogger.h"
#include "BatchSimulator.h"
#include "BenchmarkRunner.h"

// ── Format Helpers ────────────────────────────────────────────────────────────
static std::string fMs(double v) {
    if(v <= 0.01) return "\"--\"";
    char buf[32]; snprintf(buf, sizeof(buf), "\"%.2f\"", v);
    return std::string(buf);
}
static std::string fPct(double v) {
    char buf[32]; snprintf(buf, sizeof(buf), "\"%d%%\"", (int)v);
    return std::string(buf);
}

// ── JSON Extractors ───────────────────────────────────────────────────────────
static double getNum(const std::string& json, const std::string& key) {
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return -1;
    pos = json.find(':', pos);
    if (pos == std::string::npos) return -1;
    size_t start = pos + 1;
    while (start < json.size() && (json[start] == ' ' || json[start] == '\t')) ++start;
    return std::stod(json.substr(start));
}

static std::string getStr(const std::string& json, const std::string& key) {
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return "";
    pos = json.find(':', pos);
    auto q1 = json.find('"', pos + 1);
    auto q2 = json.find('"', q1 + 1);
    return json.substr(q1 + 1, q2 - q1 - 1);
}

static std::vector<std::string> splitArray(const std::string& json, const std::string& key) {
    std::vector<std::string> items;
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return items;
    auto arrStart = json.find('[', pos);
    int depth = 0; size_t itemStart = arrStart + 1;
    for (size_t i = arrStart; i < json.size(); ++i) {
        if      (json[i] == '{') { if (depth == 1) itemStart = i; depth++; }
        else if (json[i] == '}') { depth--; if (depth == 1) items.push_back(json.substr(itemStart, i - itemStart + 1)); }
        else if (json[i] == ']' && depth == 1) break;
        else if (json[i] == '[') depth++;
    }
    return items;
}

// ── Multi-Machine Parsers ─────────────────────────────────────────────────────
static std::vector<ScheduleBlock> parseNeighborSchedule(const std::string& json, const std::string& key) {
    std::vector<ScheduleBlock> schedule;
    for (const auto& item : splitArray(json, key)) {
        ScheduleBlock b;
        b.id   = getStr(item, "id");
        b.type = getStr(item, "type");
        double s = getNum(item, "start");
        double e = getNum(item, "end");
        b.start   = FuzzyNumber(s, s, s);
        b.end     = FuzzyNumber(e, e, e);
        b.dueDate = getNum(item, "due_date");
        if (!b.id.empty()) schedule.push_back(b);
    }
    return schedule;
}

static AMA* buildAMA(const std::string& json) {
    std::string amaId = getStr(json, "ama_id");
    if (amaId.empty()) return nullptr;
    auto schedule = parseNeighborSchedule(json, "ama_schedule");
    AMA* ama = new AMA(amaId, schedule);
    for (const auto& jl : splitArray(json, "job_links")) {
        std::string jobId = getStr(jl, "job_id");
        double readyTime  = getNum(jl, "ready_time");
        if (!jobId.empty() && readyTime >= 0) ama->jobCompletionTimes[jobId] = readyTime;
    }
    return ama;
}

static AMV* buildAMV(const std::string& json) {
    std::string amvId = getStr(json, "amv_id");
    if (amvId.empty()) return nullptr;
    auto schedule = parseNeighborSchedule(json, "amv_schedule");
    AMV* amv = new AMV(amvId, schedule);
    for (const auto& jl : splitArray(json, "job_links")) {
        std::string jobId          = getStr(jl, "job_id");
        double expectedStartOnNext = getNum(jl, "expected_start_on_next");
        if (!jobId.empty() && expectedStartOnNext >= 0)
            amv->expectedArrivalTimes[jobId] = expectedStartOnNext;
    }
    return amv;
}

static std::string stabilityJson(const StabilityMetrics& m) {
    std::ostringstream j;
    j << std::fixed << "{\"stable\":" << m.stable_percent << ",\"improved\":" << m.improved_percent << ",\"deteriorated\":" << m.deteriorated_percent << "}";
    return j.str();
}

static std::string splitJson(const SplitMetrics& s) {
    std::ostringstream j;
    j << std::fixed << "{\"single_ms\":" << s.single_ms << ",\"multi_ms\":"  << s.multi_ms << "}";
    return j.str();
}

// ══════════════════════════════════════════════════════════════════════════════
// MAIN ENTRY POINT
// ══════════════════════════════════════════════════════════════════════════════
int main() {
    std::string input;
    std::ostringstream buf;
    buf << std::cin.rdbuf();
    input = buf.str();
    input.erase(std::remove_if(input.begin(), input.end(),
            [](char c){ return c == '\n' || c == '\r'; }), input.end());

    if (input.empty() || input.find('{') == std::string::npos) {
        jsonLog("SYS", "ERROR: No JSON received from Kotlin UI.", "error");
        return 1;
    }

    std::string mode = getStr(input, "mode");
    if (mode.empty()) mode = "single";

    // ── ROUTE 1: BATCH MODE (Required for UI Dashboard & Loops) ───────────────
    if (mode == "batch") {
        BatchParams params;
        params.num_machines  = static_cast<int>(getNum(input, "machines"));
        params.num_jobs      = static_cast<int>(getNum(input, "jobs"));
        params.num_arhs      = static_cast<int>(getNum(input, "arhs"));
        params.w1            = getNum(input, "w1");
        params.w2            = getNum(input, "w2");
        params.num_scenarios = static_cast<int>(getNum(input, "scenarios"));

        BatchSimulationResult result = BatchSimulator::runBatch(params);

        std::ostringstream json;
        json << std::fixed << "{\"type\":\"batch_result\""
             << ",\"stability\":{\"single\":" << stabilityJson(result.stability_index.single_machine)
             << ",\"multi\":" << stabilityJson(result.stability_index.multi_machine) << "}"
             << ",\"recommendation\":\"" << result.ai_recommendation << "\""
             << ",\"reactivity\":[";
        for (size_t i = 0; i < result.reactivity_chart_data.size(); ++i) {
            const auto& d = result.reactivity_chart_data[i];
            if (i > 0) json << ",";
            json << "{\"jobs\":" << d.num_jobs << ",\"single_ms\":" << d.single_machine_time_ms << ",\"multi_ms\":"  << d.multi_machine_time_ms << "}";
        }
        json << "],\"splits\":{\"debut\":" << splitJson(result.debut_splits) << ",\"milieu\":" << splitJson(result.milieu_splits) << ",\"fin\":" << splitJson(result.fin_splits) << "}";

        std::string safeCsv = result.csv_export_data;
        for (size_t p = 0; (p = safeCsv.find('\n', p)) != std::string::npos; p += 2)
            safeCsv.replace(p, 1, "\\n");

        json << ",\"csv_data\":\"" << safeCsv << "\"}";

        std::cout << json.str() << std::endl;
        return 0;
    }

    // ── ROUTE 2: ACADEMIC BENCHMARK MODE (All-in-one run) ─────────────────────
    if (mode == "academic_benchmark") {
        std::ostringstream json;
        json << "{\"type\":\"academic_benchmark\",";

        // --- Table 4.6 ---
        json << "\"table46\":[";
        std::vector<int> m0s = {5, 10, 20};
        std::vector<int> m1s = {20, 50, 100};
        std::vector<int> m4s = {2, 4, 8};
        bool first46 = true;

        for (int m0: m0s) {
            for (int m1: m1s) {
                for (int m4: m4s) {
                    BatchParams pSOM = {m0, m1, m4, 0.3, 0.7, 100};
                    BatchParams pSOP = {m0, m1, m4, 0.7, 0.3, 100};
                    auto som = BatchSimulator::runBatch(pSOM);
                    auto sop = BatchSimulator::runBatch(pSOP);

                    if (!first46) json << ",";
                    first46 = false;

                    json << "{\"m0\":\"" << m0 << "\",\"m1\":\"" << m1 << "\",\"m4\":\"" << m4 << "\","
                         << "\"s_debut_som\":" << fMs(som.debut_splits.single_ms) << ",\"s_debut_sop\":" << fMs(sop.debut_splits.single_ms) << ","
                         << "\"s_milieu_som\":" << fMs(som.milieu_splits.single_ms) << ",\"s_milieu_sop\":" << fMs(sop.milieu_splits.single_ms) << ","
                         << "\"s_fin_som\":" << fMs(som.fin_splits.single_ms) << ",\"s_fin_sop\":" << fMs(sop.fin_splits.single_ms) << ","
                         << "\"m_debut_som\":" << fMs(som.debut_splits.multi_ms) << ",\"m_debut_sop\":" << fMs(sop.debut_splits.multi_ms) << ","
                         << "\"m_milieu_som\":" << fMs(som.milieu_splits.multi_ms) << ",\"m_milieu_sop\":" << fMs(sop.milieu_splits.multi_ms) << ","
                         << "\"m_fin_som\":" << fMs(som.fin_splits.multi_ms) << ",\"m_fin_sop\":" << fMs(sop.fin_splits.multi_ms) << "}";
                }
            }
        }
        json << "],";

        // --- Table 4.7 ---
        json << "\"table47\":[";
        bool first47 = true;
        for (int m4: m4s) {
            BatchParams pSOM = {20, 60, m4, 0.3, 0.7, 100};
            BatchParams pSOP = {20, 60, m4, 0.7, 0.3, 100};
            auto som = BatchSimulator::runBatch(pSOM);
            auto sop = BatchSimulator::runBatch(pSOP);

            if (!first47) json << ",";
            first47 = false;

            json << "{\"m4\":\"" << m4 << "\","
                 << "\"s_s_som\":" << fPct(som.stability_index.single_machine.stable_percent) << ",\"s_s_sop\":" << fPct(sop.stability_index.single_machine.stable_percent) << ","
                 << "\"s_a_som\":" << fPct(som.stability_index.single_machine.improved_percent) << ",\"s_a_sop\":" << fPct(sop.stability_index.single_machine.improved_percent) << ","
                 << "\"s_d_som\":" << fPct(som.stability_index.single_machine.deteriorated_percent) << ",\"s_d_sop\":" << fPct(sop.stability_index.single_machine.deteriorated_percent) << ","
                 << "\"m_s_som\":" << fPct(som.stability_index.multi_machine.stable_percent) << ",\"m_s_sop\":" << fPct(sop.stability_index.multi_machine.stable_percent) << ","
                 << "\"m_a_som\":" << fPct(som.stability_index.multi_machine.improved_percent) << ",\"m_a_sop\":" << fPct(sop.stability_index.multi_machine.improved_percent) << ","
                 << "\"m_d_som\":" << fPct(som.stability_index.multi_machine.deteriorated_percent) << ",\"m_d_sop\":" << fPct(sop.stability_index.multi_machine.deteriorated_percent) << "}";
        }
        json << "]}";

        std::cout << json.str() << std::endl;
        return 0;
    }

    // ── ROUTE 3: SINGLE/MULTI MODE (Required for UI Gantt Charts) ─────────────
    double alertTime       = getNum(input, "alert_time");
    double schedulingStart = getNum(input, "scheduling_start");
    double w1              = getNum(input, "w1");
    double w2              = getNum(input, "w2");
    double rulMin          = getNum(input, "rul_min");
    double rulProb         = getNum(input, "rul_prob");
    double rulMax          = getNum(input, "rul_max");
    std::string strategy   = getStr(input, "strategy");

    std::vector<ProductionJob> jobs;
    for (const auto& ji : splitArray(input, "jobs"))
        jobs.push_back({getStr(ji, "id"), getNum(ji, "duration"), getNum(ji, "due_date")});

    std::vector<TBMBlock> tbmBlocks;
    for (const auto& ti : splitArray(input, "tbm_blocks"))
        tbmBlocks.push_back({getStr(ti, "id"), getNum(ti, "start"), getNum(ti, "end")});

    std::vector<ARH> arhList;
    for (const auto& ai : splitArray(input, "arh_agents")) {
        ARH arh;
        arh.id             = getStr(ai, "id");
        arh.availabilities = {{getNum(ai, "avail_start"), getNum(ai, "avail_end")}};
        arh.repairDuration = FuzzyNumber(getNum(ai, "dur_min"), getNum(ai, "dur_prob"), getNum(ai, "dur_max"));
        arh.competencies   = {"Mechanical"};
        arhList.push_back(arh);
    }

    AMC  amc;
    ASRH asrh(arhList);
    AMS  ams(&amc, &asrh, alertTime, w1, w2);

    AMA* ama = nullptr;
    AMV* amv = nullptr;

    if (mode == "multi") {
        ama = buildAMA(input);
        amv = buildAMV(input);
        if (ama && amv) ams.setNeighbors(ama, amv);
        else mode = "single";
    }

    ams.handleAnomaly(schedulingStart, jobs, tbmBlocks, strategy, rulMin, rulProb, rulMax, mode);

    delete ama;
    delete amv;
    return 0;
}