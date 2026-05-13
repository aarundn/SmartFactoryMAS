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
#include "JsonLogger.h"

static double getNum(const std::string& json, const std::string& key) {
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return -1;
    pos = json.find(':', pos); return std::stod(json.substr(pos+1));
}

static std::string getStr(const std::string& json, const std::string& key) {
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return "";
    pos = json.find(':', pos); auto q1 = json.find('"', pos+1); auto q2 = json.find('"', q1+1);
    return json.substr(q1+1, q2-q1-1);
}

static std::vector<std::string> splitArray(const std::string& json, const std::string& key) {
    std::vector<std::string> items;
    auto pos = json.find("\"" + key + "\"");
    if (pos == std::string::npos) return items;
    auto arrStart = json.find('[', pos);
    int depth = 0; size_t itemStart = arrStart + 1;
    for (size_t i = arrStart; i < json.size(); ++i) {
        if (json[i] == '{') { if (depth == 1) itemStart = i; depth++; }
        else if (json[i] == '}') { depth--; if (depth == 1) items.push_back(json.substr(itemStart, i - itemStart + 1)); }
        else if (json[i] == ']' && depth == 1) break;
        else if (json[i] == '[') depth++;
    }
    return items;
}

int main() {
    // قراءة بيانات واجهة المستخدم (UI) من الـ Stdin
    std::string input;
    std::ostringstream buf;
    buf << std::cin.rdbuf();
    input = buf.str();
    input.erase(std::remove_if(input.begin(), input.end(), [](char c){ return c == '\n' || c == '\r'; }), input.end());

    if (input.empty() || input.find('{') == std::string::npos) {
        jsonLog("SYS", "ERROR: No dynamic data received from Kotlin UI.", "error");
        return 1;
    }

    double alertTime = getNum(input, "alert_time");
    double schedulingStart = getNum(input, "scheduling_start");
    double w1 = getNum(input, "w1");
    double w2 = getNum(input, "w2");
    std::string strategy = getStr(input, "strategy");
    double rulMin = getNum(input, "rul_min");
    double rulProb = getNum(input, "rul_prob");
    double rulMax = getNum(input, "rul_max");

    std::vector<ProductionJob> jobs;
    for (const auto& ji : splitArray(input, "jobs")) {
        jobs.push_back({getStr(ji, "id"), getNum(ji, "duration"), getNum(ji, "due_date")});
    }

    std::vector<TBMBlock> tbmBlocks;
    for (const auto& ti : splitArray(input, "tbm_blocks")) {
        tbmBlocks.push_back({getStr(ti, "id"), getNum(ti, "start"), getNum(ti, "end")});
    }

    std::vector<ARH> arhList;
    for (const auto& ai : splitArray(input, "arh_agents")) {
        ARH arh;
        arh.id = getStr(ai, "id");
        arh.availabilities = {{getNum(ai, "avail_start"), getNum(ai, "avail_end")}};
        arh.repairDuration = FuzzyNumber(getNum(ai, "dur_min"), getNum(ai, "dur_prob"), getNum(ai, "dur_max"));
        arh.competencies = {"Mechanical"}; // المهارة الأساسية
        arhList.push_back(arh);
    }

    // إطلاق الوكلاء
    AMC amc;
    ASRH asrh(arhList);
    AMS ams(&amc, &asrh, alertTime, w1, w2);

    ams.handleAnomaly(schedulingStart, jobs, tbmBlocks, strategy, rulMin, rulProb, rulMax);

    return 0;
}