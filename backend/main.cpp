#include <iostream>
#include <vector>
#include <string>
#include <sstream>
#include "FuzzyNumber.h"
#include "DataStructures.h"
#include "AMC.h"
#include "ARH.h"
#include "ASRH.h"
#include "AMA.h"
#include "AMV.h"
#include "AMS.h"
#include "JsonLogger.h"

// ── JSON helpers ─────────────────────────────────────────────────────────────

static double getNum(const std::string& json, const std::string& key) {
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return -1;
    pos = json.find(':', pos);
    return std::stod(json.substr(pos + 1));
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

// ── Neighbor machine parsers ──────────────────────────────────────────────────

// Parses a neighbor machine's schedule blocks from a JSON array key
static std::vector<ScheduleBlock> parseNeighborSchedule(
        const std::string& json, const std::string& key)
{
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

    // Override completion times from explicit job_links if provided
    for (const auto& jl : splitArray(json, "job_links")) {
        std::string jobId   = getStr(jl, "job_id");
        double readyTime    = getNum(jl, "ready_time");
        if (!jobId.empty() && readyTime >= 0)
            ama->jobCompletionTimes[jobId] = readyTime;
    }

    return ama;
}

static AMV* buildAMV(const std::string& json) {
    std::string amvId = getStr(json, "amv_id");
    if (amvId.empty()) return nullptr;

    auto schedule = parseNeighborSchedule(json, "amv_schedule");

    AMV* amv = new AMV(amvId, schedule);

    // Override expected arrivals from explicit job_links if provided
    for (const auto& jl : splitArray(json, "job_links")) {
        std::string jobId          = getStr(jl, "job_id");
        double expectedStartOnNext = getNum(jl, "expected_start_on_next");
        if (!jobId.empty() && expectedStartOnNext >= 0)
            amv->expectedArrivalTimes[jobId] = expectedStartOnNext;
    }

    return amv;
}

// ── Entry point ───────────────────────────────────────────────────────────────

int main() {
    // Read JSON from Kotlin UI via stdin
    std::string input;
    std::ostringstream buf;
    buf << std::cin.rdbuf();
    input = buf.str();
    input.erase(std::remove_if(input.begin(), input.end(),
            [](char c){ return c == '\n' || c == '\r'; }), input.end());

    if (input.empty() || input.find('{') == std::string::npos) {
        jsonLog("SYS", "ERROR: No dynamic data received from Kotlin UI.", "error");
        return 1;
    }

    // ── Parse common fields ───────────────────────────────────────────────────
    double alertTime       = getNum(input, "alert_time");
    double schedulingStart = getNum(input, "scheduling_start");
    double w1              = getNum(input, "w1");
    double w2              = getNum(input, "w2");
    double rulMin          = getNum(input, "rul_min");
    double rulProb         = getNum(input, "rul_prob");
    double rulMax          = getNum(input, "rul_max");
    std::string strategy   = getStr(input, "strategy");
    std::string mode       = getStr(input, "mode"); // "single" or "multi"
    if (mode.empty()) mode = "single";              // default to single

    // ── Parse production jobs ─────────────────────────────────────────────────
    std::vector<ProductionJob> jobs;
    for (const auto& ji : splitArray(input, "jobs"))
        jobs.push_back({getStr(ji, "id"), getNum(ji, "duration"), getNum(ji, "due_date")});

    // ── Parse TBM blocks ──────────────────────────────────────────────────────
    std::vector<TBMBlock> tbmBlocks;
    for (const auto& ti : splitArray(input, "tbm_blocks"))
        tbmBlocks.push_back({getStr(ti, "id"), getNum(ti, "start"), getNum(ti, "end")});

    // ── Parse ARH agents ──────────────────────────────────────────────────────
    std::vector<ARH> arhList;
    for (const auto& ai : splitArray(input, "arh_agents")) {
        ARH arh;
        arh.id             = getStr(ai, "id");
        arh.availabilities = {{getNum(ai, "avail_start"), getNum(ai, "avail_end")}};
        arh.repairDuration = FuzzyNumber(
                getNum(ai, "dur_min"), getNum(ai, "dur_prob"), getNum(ai, "dur_max"));
        arh.competencies   = {"Mechanical"};
        arhList.push_back(arh);
    }

    // ── Build agents ──────────────────────────────────────────────────────────
    AMC  amc;
    ASRH asrh(arhList);
    AMS  ams(&amc, &asrh, alertTime, w1, w2);

    // ── Attach neighbors only when UI tab = "Global Factory" (mode=multi) ─────
    AMA* ama = nullptr;
    AMV* amv = nullptr;

    if (mode == "multi") {
        ama = buildAMA(input);
        amv = buildAMV(input);

        if (ama != nullptr && amv != nullptr) {
            ams.setNeighbors(ama, amv);
            jsonLog("SYS", "Multi-machine mode: AMA=" + ama->id
                    + ", AMV=" + amv->id + " attached to AMS.");
        } else {
            jsonLog("SYS",
                    "Multi-machine mode requested but ama_id/amv_id missing in input. "
                    "Falling back to single-machine.", "warn");
            mode = "single";
        }
    }

    // ── Run — AMS handles everything based on mode ────────────────────────────
    ams.handleAnomaly(schedulingStart, jobs, tbmBlocks,
            strategy, rulMin, rulProb, rulMax, mode);

    // ── Cleanup ───────────────────────────────────────────────────────────────
    delete ama;
    delete amv;

    return 0;
}