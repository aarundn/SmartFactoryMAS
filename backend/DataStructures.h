/**
 * @file DataStructures.h
 * @brief Core data structures for MAS reactive scheduling.
 */
#pragma once
#include "FuzzyNumber.h"
#include <string>
#include <vector>

struct TimeInterval { double start, end; };

struct DiagnosticResult {
    std::string status;
    FuzzyNumber estimatedRUL;
    std::string requiredCompetence;
};

struct ProductionJob {
    std::string id;
    double duration;   // p_i
    double dueDate;    // d_i
};

struct TBMBlock {
    std::string id;
    double start, end;
    double duration() const { return end - start; }
};

/// A single block in the computed schedule (production, TBM, or CBM)
struct ScheduleBlock {
    std::string id;
    std::string type;     // "PRODUCTION", "TBM", "CBM"
    FuzzyNumber start;
    FuzzyNumber end;
    double dueDate = 0;   // only for PRODUCTION
};

/// Full proposal from an ARH with computed schedule
struct CBMProposal {
    std::string arhId;
    double cbmStart;
    FuzzyNumber cbmDuration;
    std::vector<ScheduleBlock> schedule;  // computed timeline
    FuzzyNumber f1, f2, f;                // computed objectives
};
