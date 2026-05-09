#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <nlohmann/json.hpp>
#include "DataModels.h"
#include "Agents.h"

using json = nlohmann::json;

void from_json(const json& j, TimeSlot& ts) {
    j.at("start").get_to(ts.start);
    j.at("end").get_to(ts.end);
}

void from_json(const json& j, ProductionJob& pj) {
    j.at("id").get_to(pj.id);
    j.at("name").get_to(pj.name);
    j.at("duration").get_to(pj.duration);
    j.at("due_date").get_to(pj.due_date);
}

void from_json(const json& j, HumanResource& hr) {
    j.at("id").get_to(hr.id);
    j.at("competence").get_to(hr.competence);
    j.at("performance").get_to(hr.performance);
    j.at("free_slots").get_to(hr.free_slots);
}

void from_json(const json& j, MaintenanceAnomaly& ma) {
    j.at("standard_repair_time").get_to(ma.standard_repair_time);
    j.at("som_deadline").get_to(ma.som_deadline);
    j.at("sop_deadline").get_to(ma.sop_deadline);
}

int main(int argc, char* argv[]) {
    if (argc != 2) {
        std::cerr << "Usage: core_engine <SOM|SOP>\n";
        return 1;
    }

    std::string strategy = argv[1];
    if (strategy != "SOM" && strategy != "SOP") {
        std::cerr << "Invalid strategy. Use SOM or SOP.\n";
        return 1;
    }

    // Load JSON
    std::ifstream f("simulation_data.json");
    if (!f.is_open()) {
        std::cerr << "Could not open simulation_data.json\n";
        return 1;
    }

    json data;
    f >> data;

    std::vector<ProductionJob> jobs = data.at("production_jobs").get<std::vector<ProductionJob>>();
    std::vector<HumanResource> hrs = data.at("human_resources").get<std::vector<HumanResource>>();
    MaintenanceAnomaly anomaly = data.at("maintenance_anomaly").get<MaintenanceAnomaly>();

    // Initialize Agents
    AgentSuperviseurRH asrh(hrs);
    AgentMaintenance amc;
    AgentMachineSujet ams(jobs);

    std::cout << "[AMS] Anomaly Detected! Strategy: " << strategy << "\n";
    std::cout << "[AMS] Requesting Maintenance (AMC)...\n";

    // Trigger protocol
    std::vector<ExtendedProposal> proposals = amc.requestMaintenance(asrh, anomaly, strategy);

    if (proposals.empty()) {
        std::cout << "[ASRH] No proposals received from workers that fit the constraints.\n";
        return 1;
    }

    for (const auto& p : proposals) {
        std::cout << "[ARH_" << p.worker_id << "] Proposes " << p.assigned_time 
                  << " mins (start: " << p.proposed_start_time << ")\n";
    }

    std::cout << "[AMS] Evaluating proposals to minimize delay...\n";
    
    std::vector<ScheduleItem> schedule;
    ExtendedProposal best = ams.selectBestProposal(proposals, schedule);

    std::cout << "[AMS] Selected Proposal from ARH_" << best.worker_id 
              << " for " << best.assigned_time << " mins starting at " << best.proposed_start_time << "\n";

    // Export to CSV
    std::ofstream csv("schedule.csv");
    if (csv.is_open()) {
        csv << "TaskName,StartTime,EndTime\n";
        for (const auto& item : schedule) {
            csv << item.task_name << "," << item.start_time << "," << item.end_time << "\n";
        }
        std::cout << "[SYSTEM] Exported schedule to schedule.csv\n";
    } else {
        std::cerr << "[SYSTEM] Failed to write schedule.csv\n";
    }

    return 0;
}
