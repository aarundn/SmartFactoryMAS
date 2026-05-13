#pragma once
#include <iostream>
#include <string>
#include <algorithm>

/**
 * @file JsonLogger.h
 * @brief Helper for emitting structured JSONL events to stdout.
 */

// Escapes special characters in a string for safe JSON embedding
inline std::string jsonEscape(const std::string& s) {
    std::string out;
    for (char c : s) {
        if (c == '"')  out += "\\\"";
        else if (c == '\\') out += "\\\\";
        else if (c == '\n') out += "\\n";
        else if (c == '\r') out += "\\r";
        else if (c == '\t') out += "\\t";
        else out += c;
    }
    return out;
}

// Emits a structured log line to stdout
inline void jsonLog(
    const std::string& agent,
    const std::string& msg,
    const std::string& level = "info",
    int step = -1)
{
    std::cout << "{\"type\":\"log\""
              << ",\"agent\":\"" << jsonEscape(agent) << "\""
              << ",\"msg\":\""   << jsonEscape(msg)   << "\""
              << ",\"level\":\"" << level << "\"";
    if (step >= 0) std::cout << ",\"step\":" << step;
    std::cout << "}" << std::endl;
}

// Emits a raw pre-built JSON object (for proposal and result events)
inline void jsonEmit(const std::string& jsonObject) {
    std::cout << jsonObject << std::endl;
}
