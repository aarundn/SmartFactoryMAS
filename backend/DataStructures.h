#pragma once
#include <string>
#include <vector>
#include "FuzzyNumber.h"

struct TimeInterval {
    double start;
    double end;
};

struct DiagnosticResult {
    std::string status;
    FuzzyNumber estimatedRUL; 
    std::string requiredCompetence;
};

struct ProductionJob {
    std::string id;
    double duration;
    double dueDate;
};

struct TBMBlock {
    std::string id;
    double start;
    double end;
};

struct ScheduleBlock {
    std::string id;
    std::string type; 
    FuzzyNumber start;
    FuzzyNumber end;
    double dueDate = 0.0;
};

struct CBMProposal {
    std::string arhId;
    double cbmStart;
    FuzzyNumber cbmDuration;
    
    std::vector<ScheduleBlock> schedule;
    std::vector<std::vector<ScheduleBlock>> tracks; 
    
    FuzzyNumber f1;
    FuzzyNumber f2;
    FuzzyNumber f;
};
