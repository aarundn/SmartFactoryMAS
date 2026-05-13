#pragma once
#include "DataStructures.h"
#include "JsonLogger.h"

class AMC {
public:
    // Accept the RUL parameters from the UI
    DiagnosticResult analyzeAnomaly(double alertTime, double rulMin, double rulProb, double rulMax) {
        DiagnosticResult diag;
        diag.status = "CBM_Required";
        diag.requiredCompetence = "Mechanical";
        
        diag.estimatedRUL = FuzzyNumber(rulMin, rulProb, rulMax); // Dynamic!
        
        jsonLog("AMC", "Analysis complete. Required Competence: " + diag.requiredCompetence + 
                       ". RUL Deadline (Probable): " + std::to_string((int)diag.estimatedRUL.prob), "warn");
        return diag;
    }
};