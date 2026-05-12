/**
 * @file Scheduler.cpp
 * @brief Implementation of the gap-filling timeline simulator.
 */
#include "Scheduler.h"
#include <algorithm>
#include <iostream>

void Scheduler::insertDueTBMs(
    FuzzyNumber& currentTime,
    std::vector<ScheduleBlock>& result,
    const std::vector<TBMBlock>& tbms,
    size_t& tbmIdx)
{
    while (tbmIdx < tbms.size() && currentTime.prob >= tbms[tbmIdx].start - 0.001) {
        const auto& tbm = tbms[tbmIdx];
        // Wait until TBM start if we're early
        FuzzyNumber tbmStart(tbm.start, tbm.start, tbm.start);
        FuzzyNumber tbmEnd(tbm.end, tbm.end, tbm.end);
        result.push_back({tbm.id, "TBM", tbmStart, tbmEnd, 0});
        currentTime = tbmEnd;
        tbmIdx++;
    }
}

void Scheduler::placeFlexibleBlock(
    const std::string& id, const std::string& type,
    double duration, double dueDate,
    FuzzyNumber& currentTime,
    std::vector<ScheduleBlock>& result,
    const std::vector<TBMBlock>& tbms,
    size_t& tbmIdx)
{
    // Try to place the block, skipping over TBM blocks if it doesn't fit
    while (true) {
        // First insert any TBM blocks that are due
        insertDueTBMs(currentTime, result, tbms, tbmIdx);

        FuzzyNumber blockEnd = currentTime + duration;

        // Check if block fits before the next TBM
        if (tbmIdx < tbms.size() && blockEnd.prob > tbms[tbmIdx].start + 0.001) {
            // Doesn't fit — skip to after this TBM block
            const auto& tbm = tbms[tbmIdx];
            FuzzyNumber tbmStart(tbm.start, tbm.start, tbm.start);
            FuzzyNumber tbmEnd(tbm.end, tbm.end, tbm.end);
            result.push_back({tbm.id, "TBM", tbmStart, tbmEnd, 0});
            currentTime = tbmEnd;
            tbmIdx++;
            continue;
        }

        // Block fits here
        result.push_back({id, type, currentTime, blockEnd, dueDate});
        currentTime = blockEnd;
        return;
    }
}

std::vector<ScheduleBlock> Scheduler::buildSchedule(
    double startTime,
    const std::vector<ProductionJob>& jobs,
    int cbmInsertPos,
    double cbmStart,
    const FuzzyNumber& cbmDuration,
    const std::vector<TBMBlock>& tbmBlocks)
{
    std::vector<ScheduleBlock> result;
    FuzzyNumber currentTime(startTime, startTime, startTime);
    size_t tbmIdx = 0;

    for (int i = 0; i <= static_cast<int>(jobs.size()); ++i) {
        // Insert CBM at the designated position
        if (i == cbmInsertPos) {
            // Wait until CBM can start (ARH availability)
            currentTime = currentTime.maxWith(cbmStart);
            // Insert any due TBMs first
            insertDueTBMs(currentTime, result, tbmBlocks, tbmIdx);
            // Recheck after TBMs
            currentTime = currentTime.maxWith(cbmStart);

            FuzzyNumber cbmEnd = currentTime + cbmDuration;
            result.push_back({"CBM", "CBM", currentTime, cbmEnd, 0});
            currentTime = cbmEnd;
        }

        // Place the next production job
        if (i < static_cast<int>(jobs.size())) {
            placeFlexibleBlock(
                jobs[i].id, "PRODUCTION",
                jobs[i].duration, jobs[i].dueDate,
                currentTime, result, tbmBlocks, tbmIdx);
        }
    }

    // Insert any remaining TBM blocks
    insertDueTBMs(currentTime, result, tbmBlocks, tbmIdx);

    return result;
}
