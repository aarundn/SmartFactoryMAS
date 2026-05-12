/**
 * @file main.cpp
 * @brief Entry point — Chapter 4 data (Tables 4.1, 4.3, 4.4).
 *
 * Communication with Kotlin UI:
 *   stdout: negotiation logs (human-readable)
 *   stderr: JSON_RESULT line (machine-readable, parsed by CliInterop)
 *
 * Reads optional JSON from stdin for dynamic ARH configuration.
 * If no stdin, uses hardcoded Chapter 4 data.
 */
#include <iostream>
#include <vector>
#include <string>
#include <sstream>
#include "FuzzyNumber.h"
#include "DataStructures.h"
#include "AMC.h"
#include "ARH.h"
#include "ASRH.h"
#include "AMS.h"

// Tries to read JSON config from stdin; returns false if empty
bool tryReadConfig(
    double& alertTime, double& schedulingStart, double& w1, double& w2,
    std::vector<ProductionJob>& jobs,
    std::vector<TBMBlock>& tbmBlocks,
    std::vector<ARH>& arhList);

int main() {
    // ── Default: Chapter 4 data ─────────────────────────────────────────

    double alertTime = 8.0;
    double schedulingStart = 24.0;  // After P4 finishes
    double w1 = 0.75, w2 = 0.25;

    // Production jobs remaining after P4 (Table 4.1)
    std::vector<ProductionJob> jobs = {
        {"P3", 46.0, 40.0},   // duration=46, due=40
        {"P2",  6.0, 93.0},   // duration=6,  due=93
        {"P1", 21.0, 110.0}   // duration=21, due=110
    };

    // Fixed TBM blocks (cannot be moved)
    std::vector<TBMBlock> tbmBlocks = {
        {"M1", 70.0, 90.0},
        {"M2", 96.0, 103.0}
    };

    // ARH agents (Tables 4.3 and 4.4)
    std::vector<ARH> arhList;

    ARH arh1;
    arh1.id = "ARH_1";
    arh1.availabilities = {{24.0, 50.0}};
    arh1.repairDuration = FuzzyNumber(4.0, 7.0, 9.0);
    arhList.push_back(arh1);

    ARH arh2;
    arh2.id = "ARH_2";
    arh2.availabilities = {{103.0, 140.0}};
    arh2.repairDuration = FuzzyNumber(3.0, 6.0, 9.0);
    arhList.push_back(arh2);

    // ── Try to read dynamic config from stdin ────────────────────────────
    // (Override defaults if Kotlin UI sends JSON input)
    tryReadConfig(alertTime, schedulingStart, w1, w2, jobs, tbmBlocks, arhList);

    // ── Create agents and run protocol ───────────────────────────────────
    AMC amc;
    ASRH asrh(arhList);
    AMS ams(&amc, &asrh, alertTime, w1, w2);

    ams.handleAnomaly(schedulingStart, jobs, tbmBlocks);

    return 0;
}

// ── Minimal JSON parser for dynamic input ────────────────────────────────────
// Reads a simple JSON format from stdin. No external dependencies.

static std::string trim(const std::string& s) {
    size_t a = s.find_first_not_of(" \t\n\r\"");
    size_t b = s.find_last_not_of(" \t\n\r\"");
    return (a == std::string::npos) ? "" : s.substr(a, b-a+1);
}

static double getNum(const std::string& json, const std::string& key) {
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return -1;
    pos = json.find(':', pos);
    return std::stod(json.substr(pos+1));
}

static std::string getStr(const std::string& json, const std::string& key) {
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return "";
    pos = json.find(':', pos);
    auto q1 = json.find('"', pos+1);
    auto q2 = json.find('"', q1+1);
    return json.substr(q1+1, q2-q1-1);
}

static std::vector<std::string> splitArray(const std::string& json, const std::string& key) {
    std::vector<std::string> items;
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return items;
    auto arrStart = json.find('[', pos);
    if (arrStart == std::string::npos) return items;

    int depth = 0;
    size_t itemStart = arrStart + 1;
    for (size_t i = arrStart; i < json.size(); ++i) {
        if (json[i] == '{') { if (depth == 1) itemStart = i; depth++; }
        else if (json[i] == '}') {
            depth--;
            if (depth == 1) items.push_back(json.substr(itemStart, i - itemStart + 1));
        }
        else if (json[i] == ']' && depth == 1) break;
        else if (json[i] == '[') depth++;
    }
    return items;
}

bool tryReadConfig(
    double& alertTime, double& schedulingStart, double& w1, double& w2,
    std::vector<ProductionJob>& jobs,
    std::vector<TBMBlock>& tbmBlocks,
    std::vector<ARH>& arhList)
{
    // Check if stdin has data (non-blocking check)
    std::string input;
    std::ostringstream buf;

    // Read all of stdin
    buf << std::cin.rdbuf();
    input = buf.str();
    input.erase(std::remove_if(input.begin(), input.end(),
        [](char c){ return c == '\n' || c == '\r'; }), input.end());

    if (input.empty() || input.find('{') == std::string::npos)
        return false;

    std::cout << "[SYS] Received dynamic configuration from UI.\n";

    // Parse top-level fields
    double v;
    v = getNum(input, "alert_time");     if (v >= 0) alertTime = v;
    v = getNum(input, "scheduling_start"); if (v >= 0) schedulingStart = v;
    v = getNum(input, "w1");             if (v >= 0) w1 = v;
    v = getNum(input, "w2");             if (v >= 0) w2 = v;

    // Parse jobs
    auto jobItems = splitArray(input, "jobs");
    if (!jobItems.empty()) {
        jobs.clear();
        for (const auto& ji : jobItems) {
            ProductionJob j;
            j.id = getStr(ji, "id");
            j.duration = getNum(ji, "duration");
            j.dueDate = getNum(ji, "due_date");
            if (!j.id.empty()) jobs.push_back(j);
        }
    }

    // Parse TBM blocks
    auto tbmItems = splitArray(input, "tbm_blocks");
    if (!tbmItems.empty()) {
        tbmBlocks.clear();
        for (const auto& ti : tbmItems) {
            TBMBlock t;
            t.id = getStr(ti, "id");
            t.start = getNum(ti, "start");
            t.end = getNum(ti, "end");
            if (!t.id.empty()) tbmBlocks.push_back(t);
        }
    }

    // Parse ARH agents
    auto arhItems = splitArray(input, "arh_agents");
    if (!arhItems.empty()) {
        arhList.clear();
        for (const auto& ai : arhItems) {
            ARH arh;
            arh.id = getStr(ai, "id");
            double aStart = getNum(ai, "avail_start");
            double aEnd = getNum(ai, "avail_end");
            arh.availabilities = {{aStart, aEnd}};
            arh.repairDuration = FuzzyNumber(
                getNum(ai, "dur_min"),
                getNum(ai, "dur_prob"),
                getNum(ai, "dur_max")
            );
            if (!arh.id.empty()) arhList.push_back(arh);
        }
    }

    return true;
}
