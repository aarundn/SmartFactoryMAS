#pragma once
#include "DataModels.h"
#include <vector>
#include <string>

struct ValidBlock {
    double start;
    double end;
};

struct ExtendedProposal : public Proposal {
    ValidBlock available_block;
};

class AgentRessourceHumaine {
public:
    AgentRessourceHumaine(const HumanResource& hr);
    
    bool evaluateCFP(const MaintenanceAnomaly& anomaly, const TimeSlot& deadline, ExtendedProposal& out_proposal);
    int getId() const { return hr_data.id; }

private:
    HumanResource hr_data;
};

class AgentSuperviseurRH {
public:
    AgentSuperviseurRH(const std::vector<HumanResource>& hrs);
    std::vector<ExtendedProposal> broadcastCFP(const MaintenanceAnomaly& anomaly, const TimeSlot& deadline);

private:
    std::vector<AgentRessourceHumaine> workers;
};

class AgentMaintenance {
public:
    AgentMaintenance();
    std::vector<ExtendedProposal> requestMaintenance(AgentSuperviseurRH& asrh, const MaintenanceAnomaly& anomaly, const std::string& strategy);
};

struct ScheduleItem {
    std::string task_name;
    double start_time;
    double end_time;
};

class AgentMachineSujet {
public:
    AgentMachineSujet(const std::vector<ProductionJob>& jobs);
    
    // Evaluates proposals and selects the best one, outputting the final schedule
    ExtendedProposal selectBestProposal(const std::vector<ExtendedProposal>& proposals, std::vector<ScheduleItem>& out_schedule);

    void setJobs(const std::vector<ProductionJob>& j) { jobs = j; }
    std::vector<ProductionJob> getJobs() const { return jobs; }

private:
    std::vector<ProductionJob> jobs;
};
