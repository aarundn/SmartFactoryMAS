#pragma once
#include <vector>
#include <random>
#include <string>
#include "DataStructures.h"
#include "ARH.h"

/**
 * TaillardGenerator — produces synthetic benchmark instances.
 *
 * KEY FIX: The constructor now accepts an external seed so that each scenario
 * in a batch run generates a UNIQUE set of jobs.  Previously the fixed seed 42
 * meant all 100 scenarios had identical processing times, making the benchmark
 * statistically meaningless.
 */
class TaillardGenerator {
private:
    std::mt19937 rng;

public:
    // Default: use a fixed seed (for reproducibility in single-call tests)
    explicit TaillardGenerator(unsigned int seed = 42) : rng(seed) {}

    // ── Jobs ───────────────────────────────────────────────────────────────
    std::vector<ProductionJob> generateJobs(int count) {
        std::vector<ProductionJob> jobs;
        // Processing times follow a uniform distribution [10, 100] as in Taillard (1993)
        std::uniform_int_distribution<int>    durDist(10, 100);
        // Due-date tightness factor k ∈ [1.2, 2.5]  (standard Taillard parameter)
        std::uniform_real_distribution<double> kDist(1.2, 2.5);

        double currentTime = 0;
        for (int i = 1; i <= count; ++i) {
            ProductionJob job;
            job.id       = "P" + std::to_string(i);
            job.duration = durDist(rng);
            // Due date: current accumulated time + duration * tightness coefficient
            job.dueDate  = currentTime + (job.duration * kDist(rng));
            jobs.push_back(job);
            // Jobs overlap slightly (80 % utilisation rate)
            currentTime += (job.duration * 0.8);
        }
        return jobs;
    }

    // ── Human Resources ────────────────────────────────────────────────────
    std::vector<ARH> generateARHs(int count) {
        std::vector<ARH> arhs;
        // Repair durations also fuzzy: centre p, spread ±20 %
        std::uniform_int_distribution<int> durDist(5, 30);

        for (int i = 1; i <= count; ++i) {
            ARH arh;
            arh.id = "ARH_" + std::to_string(i);
            double p = durDist(rng);
            arh.repairDuration = FuzzyNumber(p * 0.8, p, p * 1.2);
            arh.availabilities = {{0.0, 1e6}};   // always available in batch tests
            arh.competencies   = {"Mechanical"};
            arhs.push_back(arh);
        }
        return arhs;
    }
};