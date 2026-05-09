#include "Agents.h"
#include <iostream>
#include <algorithm>

AgentRessourceHumaine::AgentRessourceHumaine(const HumanResource& hr) : hr_data(hr) {}

bool AgentRessourceHumaine::evaluateCFP(const MaintenanceAnomaly& anomaly, const TimeSlot& deadline, ExtendedProposal& out_proposal) {
    double assigned_time = (anomaly.standard_repair_time / hr_data.competence) * hr_data.performance;
    std::cout << "[ARH_" << hr_data.id << "] Calculates assigned time: " << assigned_time << " mins.\n";
    
    for (const auto& slot : hr_data.free_slots) {
        double valid_start = std::max(slot.start, deadline.start);
        double valid_end = std::min(slot.end, deadline.end);
        
        if (valid_end - valid_start >= assigned_time) {
            out_proposal.worker_id = hr_data.id;
            out_proposal.assigned_time = assigned_time;
            out_proposal.available_block = {valid_start, valid_end};
            out_proposal.proposed_start_time = valid_start;
            std::cout << "[ARH_" << hr_data.id << "] Proposes " << assigned_time << " mins in window [" << valid_start << ", " << valid_end << "].\n";
            return true;
        }
    }
    std::cout << "[ARH_" << hr_data.id << "] Cannot meet the deadline.\n";
    return false;
}

AgentSuperviseurRH::AgentSuperviseurRH(const std::vector<HumanResource>& hrs) {
    for (const auto& hr : hrs) {
        workers.push_back(AgentRessourceHumaine(hr));
    }
}

std::vector<ExtendedProposal> AgentSuperviseurRH::broadcastCFP(const MaintenanceAnomaly& anomaly, const TimeSlot& deadline) {
    std::vector<ExtendedProposal> proposals;
    for (auto& worker : workers) {
        ExtendedProposal p;
        if (worker.evaluateCFP(anomaly, deadline, p)) {
            proposals.push_back(p);
        }
    }
    return proposals;
}

AgentMaintenance::AgentMaintenance() {}

std::vector<ExtendedProposal> AgentMaintenance::requestMaintenance(AgentSuperviseurRH& asrh, const MaintenanceAnomaly& anomaly, const std::string& strategy) {
    std::cout << "[AMC] Anomaly Detected! Requesting maintenance with strategy: " << strategy << "\n";
    TimeSlot deadline = (strategy == "SOM") ? anomaly.som_deadline : anomaly.sop_deadline;
    return asrh.broadcastCFP(anomaly, deadline);
}

AgentMachineSujet::AgentMachineSujet(const std::vector<ProductionJob>& j) : jobs(j) {}

ExtendedProposal AgentMachineSujet::selectBestProposal(const std::vector<ExtendedProposal>& proposals, std::vector<ScheduleItem>& out_schedule) {
    std::cout << "[AMS] Evaluating " << proposals.size() << " proposals...\n";
    
    ExtendedProposal best_proposal;
    std::vector<ScheduleItem> best_schedule;
    double min_delay = -1.0;
    
    for (const auto& prop : proposals) {
        for (size_t insert_idx = 0; insert_idx <= jobs.size(); ++insert_idx) {
            std::vector<ScheduleItem> temp_schedule;
            double current_time = 0.0;
            double total_delay = 0.0;
            bool valid = true;
            
            for (size_t i = 0; i < jobs.size(); ++i) {
                if (i == insert_idx) {
                    double maint_start = std::max(current_time, prop.available_block.start);
                    double maint_end = maint_start + prop.assigned_time;
                    if (maint_end > prop.available_block.end) {
                        valid = false;
                        break;
                    }
                    temp_schedule.push_back({"Maintenance", maint_start, maint_end});
                    current_time = maint_end;
                }
                
                double job_start = current_time;
                double job_end = job_start + jobs[i].duration;
                temp_schedule.push_back({jobs[i].name, job_start, job_end});
                current_time = job_end;
                
                if (job_end > jobs[i].due_date) {
                    total_delay += (job_end - jobs[i].due_date);
                }
            }
            
            if (insert_idx == jobs.size()) {
                double maint_start = std::max(current_time, prop.available_block.start);
                double maint_end = maint_start + prop.assigned_time;
                if (maint_end > prop.available_block.end) {
                    valid = false;
                } else {
                    temp_schedule.push_back({"Maintenance", maint_start, maint_end});
                    current_time = maint_end;
                }
            }
            
            if (valid) {
                if (min_delay < 0 || total_delay < min_delay) {
                    min_delay = total_delay;
                    best_schedule = temp_schedule;
                    best_proposal = prop;
                    for (const auto& item : temp_schedule) {
                        if (item.task_name == "Maintenance") {
                            best_proposal.proposed_start_time = item.start_time;
                        }
                    }
                }
            }
        }
    }
    
    std::cout << "[AMS] Selected Worker " << best_proposal.worker_id << " with minimum total delay: " << min_delay << "\n";
    out_schedule = best_schedule;
    return best_proposal;
}
