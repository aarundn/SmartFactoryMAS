#pragma once
#include <vector>
#include <random>
#include <string>
#include <algorithm>
#include <cmath>
#include "DataStructures.h"
#include "ARH.h"

// ═══════════════════════════════════════════════════════════════════════════
//  TaillardGenerator
//
//  Generates Taillard-style benchmark instances with realistic ARH
//  availability windows following the paper's section 4.1.2 formulas.
//
//  KEY CHANGE vs old version:
//    OLD: every ARH had availability [0, 10 000] → all ARHs accepted for
//         BOTH SOM and SOP → identical proposal sets → identical timings.
//    NEW: each ARH has 2 windows derived from eq.9-14 of the paper:
//           IDmin_k  = cursor + α₁          (eq.11)
//           IDmax_k  = IDmin_k + μ·p'mi + α₂ (eq.12)
//         where α₁ ∈ [p'mi/2, 2·p'mi/3]     (eq.13)
//               α₂ ∈ [0, 2·p'mi]            (eq.14)
//               μ  ∈ {1 small, 2 medium, 3 large} – we use 2 (medium)
//         The windows are spread across the schedule horizon so that:
//           • Some ARHs are available only EARLY → qualify for SOM
//           • Some ARHs are available only LATE  → qualify for SOP
//           • Some qualify for both (large μ)
//         This creates naturally different SOM vs SOP proposal counts
//         and hence different single-machine resolution times.
// ═══════════════════════════════════════════════════════════════════════════

class TaillardGenerator {
private:
    std::mt19937 rng;

public:
    // Allow custom seed so each batch scenario gets different data
    explicit TaillardGenerator(unsigned int seed = 42) : rng(seed) {}

    // ── Production jobs (Taillard style) ────────────────────────────────
    //    Duration  ∈ [10, 100]  (uniform)
    //    Due-date calculated with tightness factor k ∈ [1.2, 2.5] (eq.6-7)
    //    currentTime advances at 80 % utilisation so some jobs will be late
    //    in the greedy sequential schedule → non-zero initial_delay baseline.
    std::vector<ProductionJob> generateJobs(int count) {
        std::vector<ProductionJob> jobs;
        jobs.reserve(count);

        std::uniform_int_distribution<int>    durDist(10, 100);
        std::uniform_real_distribution<double> kDist(1.2, 2.5);

        double currentTime = 0.0;
        for (int i = 1; i <= count; ++i) {
            ProductionJob job;
            job.id       = "P" + std::to_string(i);
            job.duration = static_cast<double>(durDist(rng));
            // Paper eq.7: d = (dr + Σp) × k/Σp  simplified to
            //             d = currentTime + duration × k
            job.dueDate  = currentTime + job.duration * kDist(rng);
            jobs.push_back(job);
            currentTime += job.duration * 0.8;   // 80 % utilisation
        }
        return jobs;
    }

    // ── ARH resources with REALISTIC availability windows ───────────────
    //    Paper section 4.1.2, equations 9-14.
    //
    //    Parameters:
    //      count         – number of ARHs (m₄ ∈ {2, 4, 8})
    //      scheduleLength – total planning horizon H
    //      mu            – interval size type: 1=small, 2=medium, 3=large
    //
    //    Each ARH receives TWO availability intervals:
    //      Window 1: in the EARLY part of the horizon (qualifies for SOM)
    //      Window 2: in the LATE  part of the horizon (qualifies for SOP)
    //    The exact positions follow eq.11-12 using the per-ARH repair time.
    std::vector<ARH> generateARHs(int count,
            double scheduleLength = 1000.0,
            int mu = 2)
    {
        std::vector<ARH> arhs;
        arhs.reserve(count);

        std::uniform_int_distribution<int>     repDist(8, 25);
        std::uniform_real_distribution<double> u01(0.0, 1.0);

        // TRH = H / NRH  (eq.9)  – average time between two interventions
        double TRH = scheduleLength / static_cast<double>(count);

        // Cursor tracks where the NEXT ARH's window begins (spread evenly)
        double cursor = 0.0;

        for (int i = 1; i <= count; ++i) {
            ARH arh;
            arh.id = "ARH_" + std::to_string(i);
            arh.competencies = {"Mechanical"};

            double p = static_cast<double>(repDist(rng));   // p'mi
            arh.repairDuration = FuzzyNumber(p * 0.8, p, p * 1.2);

            // ── Window 1: EARLY part (eq.11-12) ─────────────────────────
            {
                // α₁ ∈ [p/2, 2p/3]   (eq.13)
                double alpha1 = p / 2.0 + u01(rng) * (2.0 * p / 3.0 - p / 2.0);
                // α₂ ∈ [0, 2p]        (eq.14)
                double alpha2 = u01(rng) * 2.0 * p;

                double idMin = cursor + alpha1;
                double idMax = idMin + static_cast<double>(mu) * p + alpha2;

                // Clamp to first half of the horizon so it is "early"
                idMax = std::min(idMax, scheduleLength * 0.55);
                if (idMin < idMax)
                    arh.availabilities.push_back({idMin, idMax});
            }

            // ── Window 2: LATE part (spread into second half) ────────────
            {
                double lateOffset = scheduleLength * 0.50
                        + (static_cast<double>(i) / static_cast<double>(count))
                        * scheduleLength * 0.40;

                double alpha1 = p / 2.0 + u01(rng) * (2.0 * p / 3.0 - p / 2.0);
                double alpha2 = u01(rng) * 2.0 * p;

                double idMin = lateOffset + alpha1;
                double idMax = idMin + static_cast<double>(mu) * p + alpha2;
                idMax = std::min(idMax, scheduleLength - p);   // must fit

                if (idMin < idMax && idMin < scheduleLength)
                    arh.availabilities.push_back({idMin, idMax});
            }

            // If both windows are empty (edge-case for very short horizons),
            // fall back to a safe full-horizon window so we never crash.
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