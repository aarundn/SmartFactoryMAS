#pragma once
#include <vector>
#include "DataStructures.h"

class Scheduler {
public:
    static std::vector<ScheduleBlock> buildSchedule(
        double schedulingStart,
        const std::vector<ProductionJob>& jobs,
        double cbmStart,
        FuzzyNumber cbmDuration,
        const std::vector<TBMBlock>& tbmBlocks);
};