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

    // 1. ADD FIXED BLOCKS FIRST (These cannot move!)
    ScheduleBlock cbm;
    cbm.id = "CBM";
    cbm.type = "CBM";
    cbm.start = FuzzyNumber(cbmStart, cbmStart, cbmStart);
    cbm.end = cbm.start + cbmDuration;
    cbm.dueDate = 0.0;
    schedule.push_back(cbm);

    for(const auto& tbm : tbmBlocks) {
        ScheduleBlock b;
        b.id = tbm.id;
        b.type = "TBM";
        b.start = FuzzyNumber(tbm.start, tbm.start, tbm.start);
        b.end = FuzzyNumber(tbm.end, tbm.end, tbm.end);
        b.dueDate = 0.0;
        schedule.push_back(b);
    }

    // 2. INSERT PRODUCTION JOBS AROUND FIXED BLOCKS
    FuzzyNumber cursor(schedulingStart, schedulingStart, schedulingStart);

    for(const auto& job : jobs) {
        bool collision;
        do {
            collision = false;
            FuzzyNumber currentEnd = cursor + FuzzyNumber(job.duration, job.duration, job.duration);

            for(const auto& fixed : schedule) {
                if (fixed.type != "PRODUCTION") {
                    // If the production job overlaps with a fixed maintenance block
                    if (cursor.prob < fixed.end.prob && currentEnd.prob > fixed.start.prob) {
                        collision = true;
                        cursor = fixed.end; // Force the job to wait until maintenance finishes!
                        break;
                    }
                }
            }
        } while(collision);

        // Place the job at the safely calculated cursor
        ScheduleBlock pj;
        pj.id = job.id;
        pj.type = "PRODUCTION";
        pj.start = cursor;
        pj.end = cursor + FuzzyNumber(job.duration, job.duration, job.duration);
        pj.dueDate = job.dueDate;
        schedule.push_back(pj);

        cursor = pj.end; // Move cursor to the end of this newly placed job
    }

    // 3. SORT CHRONOLOGICALLY FOR CLEAN UI OUTPUT
    std::sort(schedule.begin(), schedule.end(), [](const auto& a, const auto& b){
        return a.start.prob < b.start.prob;
    });

    return schedule;
}