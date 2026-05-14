#pragma once
#include <string>
#include <vector>
#include "DataStructures.h"

// Links a job on AMS to its timing on neighbor machines
struct JobLink {
    std::string jobId;
    double readyTime;        // c_(i-1,k): when AMA finishes this job (upstream)
    double expectedStartOnNext; // t_(i+1,k): when AMV expects this job (downstream)
};

// A neighboring machine with its current schedule
struct NeighborMachine {
    std::string id;
    std::string role;        // "UPSTREAM" or "DOWNSTREAM"
    std::vector<ScheduleBlock> schedule;
};

// A negotiation message between machines
struct NegotiationMessage {
    std::string type;        // "M_MESSAGE" or "I_MESSAGE"
    std::string from;
    std::string to;
    std::string jobId;
    double originalTime;
    double requestedTime;
    bool accepted;
    std::string reason;
};

struct MultiMachineResult {
    std::vector<NegotiationMessage> messages;
    std::vector<ScheduleBlock> amaSchedule;   // updated upstream schedule
    std::vector<ScheduleBlock> amsSchedule;   // final AMS schedule (from phase 1)
    std::vector<ScheduleBlock> amvSchedule;   // updated downstream schedule
    bool upstreamConflict   = false;
    bool downstreamConflict = false;
};