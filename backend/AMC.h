/**
 * @file AMC.h
 * @brief Agent Maintenance (AMC) — Anomaly diagnostic agent.
 */
#pragma once
#include <string>
#include <iostream>

#include "DataStructures.h"
#include "FuzzyNumber.h"
#include "JsonLogger.h"

class AMC {
public:
    double rulMin, rulProb, rulMax;

    // Default values if not overridden
    AMC(double rMin = 100.0, double rProb = 120.0, double rMax = 140.0)
            : rulMin(rMin), rulProb(rProb), rulMax(rMax) {}

    DiagnosticResult analyzeAnomaly(double anomalyTime) {
        jsonLog("AMC", "Anomaly detected at time t=" + std::to_string((int)anomalyTime) + ".");
        jsonLog("AMC", "Analyzing sensor data... Vibration abnormal.");
        jsonLog("AMC", "Diagnostic: Condition-Based Maintenance Required. RUL=[" +
                std::to_string((int)rulMin) + ", " + std::to_string((int)rulMax) + "].");
        return DiagnosticResult{"CBM_Required", FuzzyNumber(rulMin, rulProb, rulMax), "Mechanical"};
    }
};