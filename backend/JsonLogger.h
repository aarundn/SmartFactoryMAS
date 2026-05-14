#pragma once
#include <iostream>
#include <string>

// 🌟 متغير عالمي للتحكم في الطباعة 🌟
inline bool g_silentMode = false;

inline void jsonEmit(const std::string& jsonString) {
    if (!g_silentMode) { // لن يتم الطباعة إذا كنا في وضع الاختبار (Batch)
        std::cout << jsonString << std::endl;
    }
}

inline void jsonLog(const std::string& agent, const std::string& msg, const std::string& level = "info", int step = 0) {
    if (g_silentMode) return; // الخروج فوراً في وضع الاختبار لتسريع الأداء

    std::string safeMsg = msg;
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