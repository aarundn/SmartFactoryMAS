#pragma once
#include <string>
#include <vector>
#include "FuzzyNumber.h"
#include <map>

struct ReactivityData {
    int num_jobs;
    double single_machine_time_ms;
    double multi_machine_time_ms;
};

struct StabilityData {
    double stable_percent;       // لم يتغير التأخير الكلي (امتصاص كامل)
    double improved_percent;     // تحسن الجدول
    double deteriorated_percent; // زاد التأخير بشكل لا يمكن امتصاصه
};

struct BatchSimulationResult {
    std::vector<ReactivityData> reactivity_chart_data;
    StabilityData stability_index;
    std::string ai_recommendation;
    std::string csv_export_data; // لتصدير البيانات الخام إذا لزم الأمر
};

struct BatchParams {
    int num_machines;  // مثلاً 5, 10, 20
    int num_jobs;      // كثافة المهام مثلاً 65
    int num_arhs;      // عدد الخبراء مثلاً 4
    double w1;         // وزن SOP (الإنتاج)
    double w2;         // وزن SOM (الصيانة)
    int num_scenarios; // عدد السيناريوهات مثلاً 100
};
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
