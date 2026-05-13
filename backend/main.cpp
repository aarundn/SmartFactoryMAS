/**
 * @file main.cpp
 * @brief Entry point with dynamic RUL support.
 */
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
#include "AMS.h"
#include "JsonLogger.h"

bool tryReadConfig(
        double& alertTime, double& schedulingStart, double& w1, double& w2,
        double& rulMin, double& rulProb, double& rulMax, std::string& strategy,
        std::vector<ProductionJob>& jobs, std::vector<TBMBlock>& tbmBlocks, std::vector<ARH>& arhList);

int main() {
    double alertTime = 8.0;
    double schedulingStart = 24.0;
    double w1 = 0.75, w2 = 0.25;
    double rulMin = 100.0, rulProb = 120.0, rulMax = 140.0;
    std::string strategy = "SOM";

    std::vector<ProductionJob> jobs = {
            {"P3", 46.0, 40.0}, {"P2",  6.0, 93.0}, {"P1", 21.0, 110.0}
    };
    std::vector<TBMBlock> tbmBlocks = {
            {"M1", 70.0, 90.0}, {"M2", 96.0, 103.0}
    };
    std::vector<ARH> arhList;
    ARH arh1; arh1.id = "ARH_1"; arh1.availabilities = {{0.0, 50.0}};
    arh1.repairDuration = FuzzyNumber(4.0, 7.0, 9.0); arh1.competencies = {"Mechanical"};
    arhList.push_back(arh1);

    // Try to read dynamic config from stdin
    tryReadConfig(alertTime, schedulingStart, w1, w2, rulMin, rulProb, rulMax, strategy, jobs, tbmBlocks, arhList);

    AMC amc(rulMin, rulProb, rulMax);
    ASRH asrh(arhList);
    AMS ams(&amc, &asrh, alertTime, w1, w2);

    ams.handleAnomaly(schedulingStart, jobs, tbmBlocks, strategy);
    return 0;
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
        double& rulMin, double& rulProb, double& rulMax, std::string& strategy,
        std::vector<ProductionJob>& jobs, std::vector<TBMBlock>& tbmBlocks, std::vector<ARH>& arhList)
{
    std::string input; std::ostringstream buf;
    buf << std::cin.rdbuf(); input = buf.str();
    input.erase(std::remove_if(input.begin(), input.end(), [](char c){ return c == '\n' || c == '\r'; }), input.end());

    if (input.empty() || input.find('{') == std::string::npos) return false;

    double v;
    v = getNum(input, "alert_time");       if (v >= 0) alertTime = v;
    v = getNum(input, "scheduling_start"); if (v >= 0) schedulingStart = v;
    v = getNum(input, "w1");               if (v >= 0) w1 = v;
    v = getNum(input, "w2");               if (v >= 0) w2 = v;
    v = getNum(input, "rul_min");          if (v >= 0) rulMin = v;
    v = getNum(input, "rul_prob");         if (v >= 0) rulProb = v;
    v = getNum(input, "rul_max");          if (v >= 0) rulMax = v;

    std::string s = getStr(input, "strategy"); if (!s.empty()) strategy = s;

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

    auto arhItems = splitArray(input, "arh_agents");
    if (!arhItems.empty()) {
        arhList.clear();
        for (const auto& ai : arhItems) {
            ARH arh;
            arh.id = getStr(ai, "id");
            arh.availabilities = {{getNum(ai, "avail_start"), getNum(ai, "avail_end")}};
            arh.repairDuration = FuzzyNumber(getNum(ai, "dur_min"), getNum(ai, "dur_prob"), getNum(ai, "dur_max"));
            arh.competencies = {"Mechanical"};
            if (!arh.id.empty()) arhList.push_back(arh);
        }
    }
    return true;
}