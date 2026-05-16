#include "Scheduler.h"
#include <algorithm>

std::vector<ScheduleBlock> Scheduler::buildSchedule(
        double schedulingStart,
        const std::vector<ProductionJob>& jobs,
        double cbmStart,
        FuzzyNumber cbmDuration,
        const std::vector<TBMBlock>& tbmBlocks)
{
    std::vector<ScheduleBlock> schedule;

    // 1. ADD FIXED BLOCKS FIRST (Maintenance cannot move)
    if (cbmStart >= 0.0) {

    ScheduleBlock cbm;
    cbm.id = "CBM";
    cbm.type = "CBM";
    cbm.start = FuzzyNumber(cbmStart, cbmStart, cbmStart);
    cbm.end = cbm.start + cbmDuration;
    cbm.dueDate = 0.0;
    schedule.push_back(cbm);
    }
    for(const auto& tbm : tbmBlocks) {
        ScheduleBlock b;
        b.id = tbm.id;
        b.type = "TBM";
        b.start = FuzzyNumber(tbm.start, tbm.start, tbm.start);
        b.end = FuzzyNumber(tbm.end, tbm.end, tbm.end);
        b.dueDate = 0.0;
        schedule.push_back(b);
    }

    // 2. INSERT PRODUCTION JOBS (Earliest Available Gap Algorithm)
    for(const auto& job : jobs) {
        // Start searching from the very beginning for EVERY job
        FuzzyNumber testCursor(schedulingStart, schedulingStart, schedulingStart);
        bool collision;

        do {
            collision = false;
            FuzzyNumber testEnd = testCursor + FuzzyNumber(job.duration, job.duration, job.duration);

            // Check for collisions against ALL placed blocks (Maintenance AND Production)
            for(const auto& blk : schedule) {
                if (testCursor.prob < blk.end.prob && testEnd.prob > blk.start.prob) {
                    collision = true;
                    testCursor = blk.end; // Jump to the end of the block we hit and try again
                    break;
                }
            }
        } while(collision);

        // testCursor has successfully found the earliest free gap! Place the job here.
        ScheduleBlock pj;
        pj.id = job.id;
        pj.type = "PRODUCTION";
        pj.start = testCursor;
        pj.end = testCursor + FuzzyNumber(job.duration, job.duration, job.duration);
        pj.dueDate = job.dueDate;
        schedule.push_back(pj);
    }

    // 3. SORT CHRONOLOGICALLY FOR CLEAN UI GANTT CHART OUTPUT
    std::sort(schedule.begin(), schedule.end(), [](const auto& a, const auto& b){
        return a.start.prob < b.start.prob;
    });

    return schedule;
}