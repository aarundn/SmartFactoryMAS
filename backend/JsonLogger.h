#pragma once
#include <iostream>
#include <string>

inline void jsonEmit(const std::string& jsonString) {
    std::cout << jsonString << std::endl;
}

inline void jsonLog(const std::string& agent, const std::string& msg, const std::string& level = "info", int step = 0) {
    std::string safeMsg = msg;
    // استبدال علامات الاقتباس لتجنب كسر الـ JSON
    size_t pos = 0;
    while ((pos = safeMsg.find("\"", pos)) != std::string::npos) {
        safeMsg.replace(pos, 1, "\\\"");
        pos += 2;
    }
    
    std::string logLine = "{\"type\":\"log\",\"agent\":\"" + agent + 
                          "\",\"msg\":\"" + safeMsg + 
                          "\",\"level\":\"" + level + 
                          "\",\"step\":" + std::to_string(step) + "}";
    jsonEmit(logLine);
}
