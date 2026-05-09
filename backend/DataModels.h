#pragma once
#include <string>
#include <vector>

struct TimeSlot {
    double start;
    double end;
};

struct ProductionJob {
    int id;
    std::string name;
    double duration;
    double due_date;
};

struct HumanResource {
    int id;
    double competence;
    double performance;
    std::vector<TimeSlot> free_slots;
};

struct MaintenanceAnomaly {
    double standard_repair_time;
    TimeSlot som_deadline;
    TimeSlot sop_deadline;
};

struct Proposal {
    int worker_id;
    double assigned_time;
    double proposed_start_time;
};
