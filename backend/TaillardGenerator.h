#pragma once
#include <vector>
#include <random>
#include <string>
#include "DataStructures.h"
#include "ARH.h"

class TaillardGenerator {
private:
    std::mt19937 rng;

public:
    TaillardGenerator() : rng(42) {} // Seed ثابت لضمان نفس النتائج عند المقارنة

    std::vector<ProductionJob> generateJobs(int count) {
        std::vector<ProductionJob> jobs;
        std::uniform_int_distribution<int> durDist(10, 100);
        std::uniform_real_distribution<double> kDist(1.2, 2.5); // معيار Taillard لتضييق الوقت

        double currentTime = 0;
        for (int i = 1; i <= count; ++i) {
            ProductionJob job;
            job.id = "P" + std::to_string(i);
            job.duration = durDist(rng);
            job.dueDate = currentTime + (job.duration * kDist(rng));
            jobs.push_back(job);
            currentTime += (job.duration * 0.8); // المهام تتداخل قليلاً
        }
        return jobs;
    }

    std::vector<ARH> generateARHs(int count) {
        std::vector<ARH> arhs;
        std::uniform_int_distribution<int> durDist(5, 30);

        for (int i = 1; i <= count; ++i) {
            ARH arh;
            arh.id = "ARH_" + std::to_string(i);
            double p = durDist(rng);
            arh.repairDuration = FuzzyNumber(p * 0.8, p, p * 1.2);
            arh.availabilities = {{0.0, 10000.0}}; // متاحون دائماً للاختبار
            arh.competencies = {"Mechanical"};
            arhs.push_back(arh);
        }
        return arhs;
    }
};