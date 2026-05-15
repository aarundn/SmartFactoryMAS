#pragma once
#include <fstream>
#include <sstream>
#include <vector>
#include <string>
#include <stdexcept>
#include "DataStructures.h"

/**
 * TaillardParser - Reads standardized benchmark files from Taillard (1993)
 * Format: Each line contains job processing times for each machine
 * Used by researchers worldwide for scheduling algorithm comparison
 */
class TaillardParser {
public:
    struct TaillardInstance {
        std::string instanceName;
        int numJobs;
        int numMachines;
        std::vector<std::vector<int>> processingTimes; // [job][machine]
        std::vector<std::vector<int>> machineOrder;    // [job][machine sequence]
    };

    /**
     * Parse a Taillard benchmark file
     * File format:
     * Line 1: numJobs numMachines
     * Lines 2 to numJobs+1: processing times for each job across all machines
     * Lines numJobs+2 to 2*numJobs+1: machine order for each job
     */
    static TaillardInstance parseFile(const std::string& filepath) {
        std::ifstream file(filepath);
        if (!file.is_open()) {
            throw std::runtime_error("Cannot open Taillard file: " + filepath);
        }

        TaillardInstance instance;
        instance.instanceName = extractInstanceName(filepath);

        // Read dimensions
        file >> instance.numJobs >> instance.numMachines;

        // Read processing times
        instance.processingTimes.resize(instance.numJobs);
        for (int j = 0; j < instance.numJobs; ++j) {
            instance.processingTimes[j].resize(instance.numMachines);
            for (int m = 0; m < instance.numMachines; ++m) {
                file >> instance.processingTimes[j][m];
            }
        }

        // Read machine order (if present in file)
        instance.machineOrder.resize(instance.numJobs);
        for (int j = 0; j < instance.numJobs; ++j) {
            instance.machineOrder[j].resize(instance.numMachines);
            for (int m = 0; m < instance.numMachines; ++m) {
                if (file >> instance.machineOrder[j][m]) {
                    // Machine indices in Taillard are 0-based or 1-based depending on file
                    // Normalize to 0-based
                    if (instance.machineOrder[j][m] > 0) {
                        instance.machineOrder[j][m]--;
                    }
                } else {
                    // If machine order not specified, use sequential order
                    instance.machineOrder[j][m] = m;
                }
            }
        }

        file.close();
        return instance;
    }

    /**
     * Convert Taillard instance to our ProductionJob format for a specific machine
     */
    static std::vector<ProductionJob> toProductionJobs(
            const TaillardInstance& instance,
            int machineIndex,
            double dueDateTightness = 1.5) {

        std::vector<ProductionJob> jobs;
        double cumulativeTime = 0.0;

        for (int j = 0; j < instance.numJobs; ++j) {
            ProductionJob job;
            job.id = "J" + std::to_string(j + 1);
            job.duration = instance.processingTimes[j][machineIndex];

            // Due date calculation following Taillard methodology
            // DueDate = CurrentTime + Duration * TightnessCoefficient
            // Tightness 1.3 = very tight, 2.0 = loose
            job.dueDate = cumulativeTime + (job.duration * dueDateTightness);

            jobs.push_back(job);
            cumulativeTime += job.duration;
        }

        return jobs;
    }

    /**
     * Generate a synthetic Taillard-style instance (for testing when files unavailable)
     */
    static TaillardInstance generateSynthetic(
            const std::string& name,
            int numJobs,
            int numMachines,
            int minTime = 1,
            int maxTime = 99,
            unsigned int seed = 42) {

        std::mt19937 rng(seed);
        std::uniform_int_distribution<int> timeDist(minTime, maxTime);

        TaillardInstance instance;
        instance.instanceName = name;
        instance.numJobs = numJobs;
        instance.numMachines = numMachines;

        instance.processingTimes.resize(numJobs);
        instance.machineOrder.resize(numJobs);

        for (int j = 0; j < numJobs; ++j) {
            instance.processingTimes[j].resize(numMachines);
            instance.machineOrder[j].resize(numMachines);

            for (int m = 0; m < numMachines; ++m) {
                instance.processingTimes[j][m] = timeDist(rng);
                instance.machineOrder[j][m] = m; // Sequential flow
            }
        }

        return instance;
    }

private:
    static std::string extractInstanceName(const std::string& filepath) {
        size_t lastSlash = filepath.find_last_of("/\\");
        size_t lastDot = filepath.find_last_of('.');

        if (lastSlash == std::string::npos) lastSlash = 0;
        else lastSlash++;

        if (lastDot == std::string::npos || lastDot < lastSlash) {
            lastDot = filepath.length();
        }

        return filepath.substr(lastSlash, lastDot - lastSlash);
    }
};