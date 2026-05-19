#pragma once
#include <vector>
#include <random>
#include <string>
#include <algorithm>
#include <cmath>
#include "DataStructures.h"
#include "ARH.h"

// ═══════════════════════════════════════════════════════════════════════════
//  TaillardGenerator — EXACT 1993 Taillard Benchmark Implementation
//
//  Reference: E. Taillard (1993), "Benchmarks for basic scheduling problems",
//             European Journal of Operational Research 64, pp. 278-285.
//
//  This generator uses the EXACT Linear Congruential Generator (LCG) and
//  the EXACT seeds from the original Taillard paper to produce deterministic
//  benchmark instances. Processing times are integers in [1, 99].
//
//  The thesis (Bouzidi-Hassini) states in Section 4.1.1:
//    "Pour chaque benchmark, nous testons 10 instances"
//  She used 10 instances per (jobs, machines) configuration.
//
//  Instance groups used by the thesis (Table 4.6 & 4.7):
//    ta001-ta010: 20 jobs,   5 machines
//    ta011-ta020: 20 jobs,  10 machines 
//    ta021-ta030: 20 jobs,  20 machines
//    ta031-ta040: 50 jobs,   5 machines
//    ta041-ta050: 50 jobs,  10 machines
//    ta051-ta060: 50 jobs,  20 machines
//    ta061-ta070: 100 jobs,  5 machines
//    ta071-ta080: 100 jobs, 10 machines
//    ta081-ta090: 100 jobs, 20 machines
// ═══════════════════════════════════════════════════════════════════════════

class TaillardGenerator {
private:
    std::mt19937 rng;  // For ARH generation (not part of Taillard standard)

    // ── The EXACT Taillard LCG (from the 1993 paper) ─────────────────────
    // This is the deterministic PRNG that produces the same processing times
    // on every machine, every OS, every compiler. It is NOT std::mt19937.
    static int taillard_unif(long& seed, int low, int high) {
        static const long m = 2147483647;
        static const long a = 16807;
        static const long b = 127773;
        static const long c = 2836;

        long k = seed / b;
        seed = a * (seed % b) - k * c;
        if (seed < 0) seed = seed + m;
        double value_0_1 = static_cast<double>(seed) / static_cast<double>(m);
        return static_cast<int>(low + std::floor(value_0_1 * (high - low + 1)));
    }

    // ── Official Taillard 1993 seeds ─────────────────────────────────────
    // These are the EXACT seeds from OR-Library (flowshop2.txt).
    // Each seed deterministically generates one benchmark instance.
    struct TaillardInstance {
        long seed;
        int  num_jobs;
        int  num_machines;
    };

    static const std::vector<TaillardInstance>& getInstances() {
        static const std::vector<TaillardInstance> instances = {
            // ── 20 jobs, 5 machines (ta001-ta010) ────────────────────────
            { 873654221, 20,  5},  // ta001
            { 379008056, 20,  5},  // ta002
            {1866992158, 20,  5},  // ta003
            { 216771124, 20,  5},  // ta004
            { 495070989, 20,  5},  // ta005
            { 402959317, 20,  5},  // ta006
            {1369363414, 20,  5},  // ta007
            {2021925980, 20,  5},  // ta008
            { 573109518, 20,  5},  // ta009
            {  88325120, 20,  5},  // ta010

            // ── 20 jobs, 10 machines (ta011-ta020) ───────────────────────
            { 587595453, 20, 10},  // ta011
            {1401007982, 20, 10},  // ta012
            { 873136276, 20, 10},  // ta013
            { 268827376, 20, 10},  // ta014
            {1634173168, 20, 10},  // ta015
            { 691823909, 20, 10},  // ta016
            {  73807235, 20, 10},  // ta017
            {1273398721, 20, 10},  // ta018
            {2065119309, 20, 10},  // ta019
            {1672900551, 20, 10},  // ta020

            // ── 20 jobs, 20 machines (ta021-ta030) ───────────────────────
            { 479340445, 20, 20},  // ta021
            { 268827376, 20, 20},  // ta022
            {1958948863, 20, 20},  // ta023
            { 918272953, 20, 20},  // ta024
            { 555010963, 20, 20},  // ta025
            {2010851491, 20, 20},  // ta026
            {1519833303, 20, 20},  // ta027
            {1748670931, 20, 20},  // ta028
            {1923497586, 20, 20},  // ta029
            {1829909967, 20, 20},  // ta030

            // ── 50 jobs, 5 machines (ta031-ta040) ────────────────────────
            {1328042058, 50,  5},  // ta031
            { 200382020, 50,  5},  // ta032
            { 496319842, 50,  5},  // ta033
            {1203030903, 50,  5},  // ta034
            {1730708564, 50,  5},  // ta035
            { 450926852, 50,  5},  // ta036
            {1303135678, 50,  5},  // ta037
            {1273398721, 50,  5},  // ta038
            { 587288402, 50,  5},  // ta039
            { 248421594, 50,  5},  // ta040

            // ── 50 jobs, 10 machines (ta041-ta050) ───────────────────────
            {1958948863, 50, 10},  // ta041
            { 575633267, 50, 10},  // ta042
            { 655816003, 50, 10},  // ta043
            {1977864101, 50, 10},  // ta044
            {  93805469, 50, 10},  // ta045
            {1803345551, 50, 10},  // ta046
            {  49612559, 50, 10},  // ta047
            {1899802599, 50, 10},  // ta048
            {2013025619, 50, 10},  // ta049
            { 578962478, 50, 10},  // ta050

            // ── 50 jobs, 20 machines (ta051-ta060) ───────────────────────
            {1539989115, 50, 20},  // ta051
            { 691823909, 50, 20},  // ta052
            { 655816003, 50, 20},  // ta053
            {1315102446, 50, 20},  // ta054
            {1949668355, 50, 20},  // ta055
            {1923497586, 50, 20},  // ta056
            {1805594913, 50, 20},  // ta057
            {1861070898, 50, 20},  // ta058
            { 715643788, 50, 20},  // ta059
            { 464843328, 50, 20},  // ta060

            // ── 100 jobs, 5 machines (ta061-ta070) ───────────────────────
            { 896678084, 100,  5}, // ta061
            {1179439976, 100,  5}, // ta062
            {1122278347, 100,  5}, // ta063
            { 416756875, 100,  5}, // ta064
            { 267829958, 100,  5}, // ta065
            {1835213917, 100,  5}, // ta066
            {1328833962, 100,  5}, // ta067
            {1418570761, 100,  5}, // ta068
            { 161033112, 100,  5}, // ta069
            { 304212574, 100,  5}, // ta070

            // ── 100 jobs, 10 machines (ta071-ta080) ──────────────────────
            {1539989115, 100, 10}, // ta071
            { 655816003, 100, 10}, // ta072
            { 960914243, 100, 10}, // ta073
            {1915696806, 100, 10}, // ta074
            {2013025619, 100, 10}, // ta075
            {1168140026, 100, 10}, // ta076
            {1923497586, 100, 10}, // ta077
            { 167698528, 100, 10}, // ta078
            {1528387973, 100, 10}, // ta079
            { 993794175, 100, 10}, // ta080

            // ── 100 jobs, 20 machines (ta081-ta090) ──────────────────────
            { 450926852, 100, 20}, // ta081
            {1462772409, 100, 20}, // ta082
            {1021685265, 100, 20}, // ta083
            {  83696007, 100, 20}, // ta084
            { 508154254, 100, 20}, // ta085
            {1861070898, 100, 20}, // ta086
            {  26482542, 100, 20}, // ta087
            { 444956424, 100, 20}, // ta088
            {2115448041, 100, 20}, // ta089
            { 118254244, 100, 20}, // ta090
        };
        return instances;
    }

    // ── Find the 10 instances matching a (jobs, machines) pair ────────────
    static std::vector<int> findInstanceIndices(int numJobs, int numMachines) {
        std::vector<int> indices;
        const auto& all = getInstances();
        for (int i = 0; i < static_cast<int>(all.size()); ++i) {
            if (all[i].num_jobs == numJobs) {
                indices.push_back(i);
            }
        }
        // If exact match not found, return first 10 available
        if (indices.empty()) {
            for (int i = 0; i < std::min(10, static_cast<int>(all.size())); ++i)
                indices.push_back(i);
        }
        return indices;
    }

public:
    // Allow custom seed for ARH generation (Taillard doesn't define ARHs)
    explicit TaillardGenerator(unsigned int seed = 42) : rng(seed) {}

    // ── Generate jobs from an EXACT Taillard instance ────────────────────
    //    instanceIndex: 0-9 within the (jobs, machines) group
    //    The processing times come from the deterministic LCG.
    //    Due dates are calculated using tightness factor k (eq.6-7 from thesis).
    std::vector<ProductionJob> generateJobsFromTaillard(int numJobs, int instanceIndex = 0) {
        // Find the correct instance group
        auto indices = findInstanceIndices(numJobs, 5); // machines don't matter for job durations
        if (instanceIndex >= static_cast<int>(indices.size()))
            instanceIndex = instanceIndex % static_cast<int>(indices.size());

        const auto& inst = getInstances()[indices[instanceIndex]];
        long seed = inst.seed;

        // Generate the processing time matrix using Taillard's LCG
        // d[machine][job] — we only need machine 0 for single-machine scheduling
        // but we sum across all machines for total job duration
        int nMach = inst.num_machines;
        int nJobs = inst.num_jobs;
        
        // Generate ALL processing times (machines × jobs) exactly as Taillard does
        std::vector<std::vector<int>> processingTimes(nMach, std::vector<int>(nJobs));
        for (int i = 0; i < nMach; ++i)
            for (int j = 0; j < nJobs; ++j)
                processingTimes[i][j] = taillard_unif(seed, 1, 99);

        // Build ProductionJob list
        // For single-machine scheduling, job duration = sum of processing times
        // across all machines (total flow time for the job)
        std::vector<ProductionJob> jobs;
        jobs.reserve(nJobs);

        double currentTime = 0.0;
        std::uniform_real_distribution<double> kDist(1.2, 2.5);

        for (int j = 0; j < nJobs; ++j) {
            ProductionJob job;
            job.id = "P" + std::to_string(j + 1);
            
            // Sum processing times across all machines for this job
            double totalDuration = 0.0;
            for (int i = 0; i < nMach; ++i)
                totalDuration += processingTimes[i][j];
            
            // Scale to reasonable single-machine duration
            // (average across machines, keeping Taillard's relative proportions)
            job.duration = totalDuration / static_cast<double>(nMach);
            
            // Due date: thesis eq.7 with tightness factor
            double k = kDist(rng);
            job.dueDate = currentTime + job.duration * k;
            jobs.push_back(job);
            currentTime += job.duration * 0.8;  // 80% utilisation
        }
        return jobs;
    }

    // ── Original random generator (kept for backward compatibility) ───────
    std::vector<ProductionJob> generateJobs(int count) {
        // For standard Taillard sizes, use the exact instances
        if (count == 20 || count == 50 || count == 100) {
            // Pick a random instance index (0-9) for variety across scenarios
            std::uniform_int_distribution<int> instDist(0, 9);
            int idx = instDist(rng);
            return generateJobsFromTaillard(count, idx);
        }
        
        // For non-standard sizes, fall back to Taillard-style random generation
        std::vector<ProductionJob> jobs;
        jobs.reserve(count);

        std::uniform_int_distribution<int>    durDist(1, 99); // Taillard range [1,99]
        std::uniform_real_distribution<double> kDist(1.2, 2.5);

        double currentTime = 0.0;
        for (int i = 1; i <= count; ++i) {
            ProductionJob job;
            job.id       = "P" + std::to_string(i);
            job.duration = static_cast<double>(durDist(rng));
            job.dueDate  = currentTime + job.duration * kDist(rng);
            jobs.push_back(job);
            currentTime += job.duration * 0.8;
        }
        return jobs;
    }

    // ── ARH resources with REALISTIC availability windows ───────────────
    //    Paper section 4.1.2, equations 9-14.
    std::vector<ARH> generateARHs(int count,
            double scheduleLength = 1000.0,
            int mu = 2)
    {
        std::vector<ARH> arhs;
        arhs.reserve(count);

        std::uniform_int_distribution<int>     repDist(8, 25);
        std::uniform_real_distribution<double> u01(0.0, 1.0);

        double TRH = scheduleLength / static_cast<double>(count);
        double cursor = 0.0;

        for (int i = 1; i <= count; ++i) {
            ARH arh;
            arh.id = "ARH_" + std::to_string(i);
            arh.competencies = {"Mechanical"};

            double p = static_cast<double>(repDist(rng));
            arh.repairDuration = FuzzyNumber(p * 0.8, p, p * 1.2);

            // ── Window 1: EARLY part (eq.11-12) ─────────────────────────
            {
                double alpha1 = p / 2.0 + u01(rng) * (2.0 * p / 3.0 - p / 2.0);
                double alpha2 = u01(rng) * 2.0 * p;
                double idMin = cursor + alpha1;
                double idMax = idMin + static_cast<double>(mu) * p + alpha2;
                idMax = std::min(idMax, scheduleLength * 0.55);
                if (idMin < idMax)
                    arh.availabilities.push_back({idMin, idMax});
            }

            // ── Window 2: LATE part ─────────────────────────────────────
            {
                double lateOffset = scheduleLength * 0.50
                        + (static_cast<double>(i) / static_cast<double>(count))
                        * scheduleLength * 0.40;
                double alpha1 = p / 2.0 + u01(rng) * (2.0 * p / 3.0 - p / 2.0);
                double alpha2 = u01(rng) * 2.0 * p;
                double idMin = lateOffset + alpha1;
                double idMax = idMin + static_cast<double>(mu) * p + alpha2;
                idMax = std::min(idMax, scheduleLength - p);
                if (idMin < idMax && idMin < scheduleLength)
                    arh.availabilities.push_back({idMin, idMax});
            }

            if (arh.availabilities.empty()) {
                double safeStart = u01(rng) * (scheduleLength * 0.8);
                arh.availabilities.push_back({safeStart, safeStart + (scheduleLength * 0.2)});
            }

            arhs.push_back(arh);
            cursor += TRH;
        }

        return arhs;
    }
};