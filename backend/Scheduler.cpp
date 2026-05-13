/**
 * @file Scheduler.cpp
 * @brief Implementation of the gap-filling timeline simulator.
 */
#include "Scheduler.h"
#include <algorithm>
#include <iostream>

#include "Scheduler.h"
#include <algorithm>
#include <iostream>

struct FixedBlock {
    std::string id;
    std::string type;
    double start;
    FuzzyNumber duration;
    
    bool operator<(const FixedBlock& other) const {
        return start < other.start;
    }
};

std::vector<ScheduleBlock> Scheduler::buildSchedule(
    double startTime,
    const std::vector<ProductionJob>& jobs,
    double cbmStart,
    const FuzzyNumber& cbmDuration,
    const std::vector<TBMBlock>& tbmBlocks)
{
    // Combine TBMs and CBM into a single sorted list of fixed blocks
    std::vector<FixedBlock> fixedBlocks;
    for (const auto& tbm : tbmBlocks) {
        fixedBlocks.push_back({tbm.id, "TBM", tbm.start, FuzzyNumber(tbm.end - tbm.start, tbm.end - tbm.start, tbm.end - tbm.start)});
    }
    fixedBlocks.push_back({"CBM", "CBM", cbmStart, cbmDuration});
    std::sort(fixedBlocks.begin(), fixedBlocks.end());
    
    std::vector<ScheduleBlock> result;
    FuzzyNumber currentTime(startTime, startTime, startTime);
    size_t fixedIdx = 0;
    
    auto insertDueFixedBlocks = [&](FuzzyNumber& currTime, std::vector<ScheduleBlock>& res, size_t& idx) {
        while (idx < fixedBlocks.size() && currTime.prob >= fixedBlocks[idx].start - 0.001) {
            const auto& fb = fixedBlocks[idx];
            FuzzyNumber start(fb.start, fb.start, fb.start);
            FuzzyNumber end = start + fb.duration;
            res.push_back({fb.id, fb.type, start, end, 0});
            currTime = end;
            idx++;
        }
    };

    for (const auto& job : jobs) {
        while (true) {
            insertDueFixedBlocks(currentTime, result, fixedIdx);
            
            FuzzyNumber blockEnd = currentTime + job.duration;
            
            // Check if block fits before the next fixed block
            if (fixedIdx < fixedBlocks.size() && blockEnd.prob > fixedBlocks[fixedIdx].start + 0.001) {
                // Doesn't fit — skip to after this fixed block
                const auto& fb = fixedBlocks[fixedIdx];
                FuzzyNumber start(fb.start, fb.start, fb.start);
                FuzzyNumber end = start + fb.duration;
                result.push_back({fb.id, fb.type, start, end, 0});
                currentTime = end;
                fixedIdx++;
                continue;
            }
            
            // Fits in the current gap
            result.push_back({job.id, "PRODUCTION", currentTime, blockEnd, job.dueDate});
            currentTime = blockEnd;
            break;
        }
    }
    
    // Insert any remaining fixed blocks at their scheduled times
    insertDueFixedBlocks(currentTime, result, fixedIdx);
    while (fixedIdx < fixedBlocks.size()) {
        const auto& fb = fixedBlocks[fixedIdx];
        FuzzyNumber start(fb.start, fb.start, fb.start);
        FuzzyNumber end = start + fb.duration;
        result.push_back({fb.id, fb.type, start, end, 0});
        currentTime = end;
        fixedIdx++;
    }

    return result;
}
