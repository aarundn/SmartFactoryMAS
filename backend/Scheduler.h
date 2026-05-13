/**
 * @file Scheduler.h
 * @brief Gap-filling timeline simulator that respects fixed TBM blocks.
 *
 * Implements the scheduling algorithm from Chapter 4 (Figures 4.6, 4.7):
 * Given a job permutation and CBM insertion point, computes the exact
 * fuzzy start/end times for all blocks on the machine timeline.
 *
 * The algorithm:
 *  1. Maintains a fuzzy currentTime cursor
 *  2. Places flexible items (CBM + production jobs) sequentially
 *  3. When currentTime reaches a TBM block, inserts it automatically
 *  4. If a job doesn't fit before the next TBM, pushes it after
 */
#pragma once
#include "DataStructures.h"
#include <vector>

class Scheduler {
public:
    /**
     * @brief Builds a complete schedule by inserting CBM at a given position.
     *
     * @param startTime      When scheduling begins (e.g., 24.0 after P4 finishes)
     * @param jobs           Production jobs in the proposed permutation order
     * @param cbmInsertPos   Index in jobs[] before which CBM is inserted
     *                       (0 = before first job, jobs.size() = after last job)
     * @param cbmStart       Earliest CBM can begin (ARH availability start)
     * @param cbmDuration    Fuzzy CBM duration
     * @param tbmBlocks      Fixed TBM blocks (M1, M2) sorted by start time
     * @return Complete schedule as a list of ScheduleBlocks
     */
    static std::vector<ScheduleBlock> buildSchedule(
        double startTime,
        const std::vector<ProductionJob>& jobs,
        double cbmStart,
        const FuzzyNumber& cbmDuration,
        const std::vector<TBMBlock>& tbmBlocks);
};
