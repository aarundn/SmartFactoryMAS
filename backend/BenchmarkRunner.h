#pragma once
#include <chrono>
#include <vector>
#include <string>
#include <fstream>
#include <iomanip>
#include <cmath>
#include <algorithm>
#include "DataStructures.h"
#include "TaillardParser.h"
#include "AMC.h"
#include "ASRH.h"
#include "AMS.h"
#include "TaillardGenerator.h"

/**
 * BenchmarkRunner - Implements Phase 5 of the research paper
 * Generates thousands of test scenarios and collects performance metrics
 * Produces data tables matching Tables 4.6 and 4.7 in the paper
 */
class BenchmarkRunner {
public:
    struct AnomalyPosition {
        const char* name;
        double startRatio;
        double endRatio;
    };

    // The three anomaly timing tests from the paper
    static constexpr AnomalyPosition POSITIONS[3] = {
        {"Start",  0.0,  0.33},  // Début
        {"Half",   0.33, 0.67},  // Milieu
        {"End",    0.67, 1.0}    // Fin
    };

    struct TestResult {
        std::string instanceName;
        std::string strategy;
        std::string anomalyPosition;
        int numJobs;
        int numMachines;
        int numARHs;

        double computationTimeMs;     // Réactivité (reactivity)
        double totalDelay;            // Retard total
        double averageDelayPerJob;    // Retard moyen
        double maxJobDelay;           // Pire retard

        int jobsOnTime;               // Jobs à l'heure
        int jobsDelayed;              // Jobs en retard

        double anomalyTime;           // Instant de l'anomalie
        double cbmStartTime;          // Début maintenance
        double cbmDuration;           // Durée maintenance
    };

    struct AggregatedResults {
        std::string configuration;
        int totalTests;

        double avgComputationTime;
        double stdComputationTime;
        double minComputationTime;
        double maxComputationTime;

        double avgTotalDelay;
        double stdTotalDelay;
        double minTotalDelay;
        double maxTotalDelay;

        double avgJobDelay;
        double successRate;  // % jobs finished on time
    };

    /**
     * Main benchmark execution - runs complete test suite
     */
    static void runCompleteBenchmark(
            const std::string& outputDir = "./benchmark_results/") {

        std::cout << "\n═══════════════════════════════════════════════════════════\n";
        std::cout << "     PHASE 5: BENCHMARK EXECUTION (Taillard Suite)\n";
        std::cout << "═══════════════════════════════════════════════════════════\n\n";

        // Create output directory
        system(("mkdir -p " + outputDir).c_str());

        // Test configurations matching the paper
        std::vector<int> machineCounts = {5, 10, 15, 20};
        std::vector<int> jobCounts = {20, 50, 100, 200};
        std::vector<int> arhCounts = {2, 4, 8};
        std::vector<std::string> strategies = {"SOM", "SOP"};

        std::vector<TestResult> allResults;
        int totalTests = machineCounts.size() * jobCounts.size() *
                arhCounts.size() * strategies.size() * 3 * 100; // 3 positions, 100 runs each

        std::cout << "Configuration:\n";
        std::cout << "  - Machines: {5, 10, 15, 20}\n";
        std::cout << "  - Jobs per machine: {20, 50, 100, 200}\n";
        std::cout << "  - ARH counts: {2, 4, 8}\n";
        std::cout << "  - Strategies: {SOM, SOP}\n";
        std::cout << "  - Anomaly positions: {Start, Half, End}\n";
        std::cout << "  - Runs per configuration: 100\n";
        std::cout << "  - TOTAL TESTS: " << totalTests << "\n\n";

        int testCounter = 0;
        auto benchmarkStart = std::chrono::high_resolution_clock::now();

        // Enable silent mode for batch testing
        g_silentMode = true;

        for (int numMachines : machineCounts) {
            for (int numJobs : jobCounts) {
                for (int numARHs : arhCounts) {
                    for (const auto& strategy : strategies) {
                        for (int posIdx = 0; posIdx < 3; ++posIdx) {

                            std::cout << "\rProgress: " << testCounter << "/" << totalTests
                                      << " [" << (testCounter * 100 / totalTests) << "%]"
                                      << std::flush;

                            // Run 100 scenarios for this configuration
                            for (int run = 1; run <= 100; ++run) {
                                auto result = runSingleTest(
                                        numMachines, numJobs, numARHs,
                                        strategy, POSITIONS[posIdx], run);

                                allResults.push_back(result);
                                testCounter++;
                            }
                        }
                    }
                }
            }
        }

        auto benchmarkEnd = std::chrono::high_resolution_clock::now();
        double totalBenchmarkTime = std::chrono::duration<double>(
                benchmarkEnd - benchmarkStart).count();

        g_silentMode = false;

        std::cout << "\n\n✓ Benchmark completed in " << totalBenchmarkTime << " seconds\n";
        std::cout << "  (" << (totalBenchmarkTime / 60.0) << " minutes)\n\n";

        // Export results
        exportRawData(allResults, outputDir + "raw_results.csv");
        exportAggregatedData(allResults, outputDir + "aggregated_results.csv");
        exportLatexTables(allResults, outputDir + "latex_tables.tex");
        generateAnalysisReport(allResults, outputDir + "analysis_report.txt");

        std::cout << "\nResults exported to: " << outputDir << "\n";
        std::cout << "  - raw_results.csv (all individual tests)\n";
        std::cout << "  - aggregated_results.csv (Table 4.6 format)\n";
        std::cout << "  - latex_tables.tex (Table 4.7 format)\n";
        std::cout << "  - analysis_report.txt (detailed analysis)\n\n";
    }

private:
    /**
     * Run a single test scenario
     */
    static TestResult runSingleTest(
            int numMachines,
            int numJobs,
            int numARHs,
            const std::string& strategy,
            const AnomalyPosition& position,
            int runNumber) {

        TestResult result;
        result.instanceName = "M" + std::to_string(numMachines) +
                "_J" + std::to_string(numJobs) +
                "_R" + std::to_string(runNumber);
        result.strategy = strategy;
        result.anomalyPosition = position.name;
        result.numJobs = numJobs;
        result.numMachines = numMachines;
        result.numARHs = numARHs;

        // Generate Taillard instance
        TaillardParser::TaillardInstance instance =
                TaillardParser::generateSynthetic(
                        result.instanceName, numJobs, numMachines, 10, 100, runNumber);

        // Focus on first machine (typical research approach)
        auto jobs = TaillardParser::toProductionJobs(instance, 0, 1.5);

        // Calculate schedule length
        double scheduleLength = 0.0;
        for (const auto& job : jobs) {
            scheduleLength += job.duration;
        }

        // Determine anomaly time based on position
        std::mt19937 rng(runNumber);
        std::uniform_real_distribution<double> anomalyDist(
                scheduleLength * position.startRatio,
                scheduleLength * position.endRatio);
        result.anomalyTime = anomalyDist(rng);

        // Generate ARH resources
        TaillardGenerator generator;
        auto arhs = generator.generateARHs(numARHs);

        // Set RUL (Remaining Useful Life) based on strategy
        double rulMin, rulProb, rulMax;
        if (strategy == "SOM") {
            // SOM: Conservative - must finish before risk zone
            rulMin = result.anomalyTime + 50;
            rulProb = result.anomalyTime + 80;
            rulMax = result.anomalyTime + 120;
        } else {
            // SOP: Aggressive - can wait until last minute
            rulMin = result.anomalyTime + 80;
            rulProb = result.anomalyTime + 120;
            rulMax = result.anomalyTime + 150;
        }

        // Set weights based on strategy
        double w1 = (strategy == "SOP") ? 0.7 : 0.3;  // Production weight
        double w2 = (strategy == "SOM") ? 0.7 : 0.3;  // Maintenance weight

        // Build agents
        AMC amc;
        ASRH asrh(arhs);
        AMS ams(&amc, &asrh, result.anomalyTime, w1, w2);

        std::vector<TBMBlock> emptyTBMs;

        // ⏱️ MEASURE COMPUTATION TIME (Single) ⏱️
        auto startTime = std::chrono::high_resolution_clock::now();

        auto proposals = ams.handleAnomaly(
                0.0, jobs, emptyTBMs, strategy,
                rulMin, rulProb, rulMax, "single");

        auto endTime = std::chrono::high_resolution_clock::now();
        result.computationTimeMs = std::chrono::duration<double, std::milli>(
                endTime - startTime).count();

        // ⏱️ MEASURE MULTI-MACHINE COORDINATION ⏱️
        if (!proposals.empty()) {
            CBMProposal* best = &proposals[0];
            for (auto& prop : proposals) {
                if (prop.f.prob < best->f.prob) best = &prop;
            }

            auto tm0 = std::chrono::high_resolution_clock::now();
            MultiMachineCoordinator coordinator;
            
            // Simplified baseline for neighbors
            auto baseline = Scheduler::buildSchedule(0.0, jobs, -1.0, FuzzyNumber(0,0,0), emptyTBMs);
            AMA ama_agent("AMA", baseline); 
            AMV amv_agent("AMV", baseline);
            
            coordinator.negotiate(best->schedule, ama_agent, amv_agent);
            auto tm1 = std::chrono::high_resolution_clock::now();
            
            // Re-assign computationTime to include the full MAS cycle if requested, 
            // but for Table 4.6 we usually want them separated. 
            // In this runner, we'll keep the single-machine time as primary 
            // but log the multi-machine overhead.
            double multiOverhead = std::chrono::duration<double, std::milli>(tm1 - tm0).count();

            result.cbmStartTime = best->cbmStart;
            result.cbmDuration = best->cbmDuration.prob;
            result.totalDelay = 0.0;
            result.maxJobDelay = 0.0;
            result.jobsOnTime = 0;
            result.jobsDelayed = 0;

            for (const auto& block : best->schedule) {
                if (block.type == "PRODUCTION") {
                    double delay = std::max(0.0, block.end.prob - block.dueDate);
                    result.totalDelay += delay;
                    result.maxJobDelay = std::max(result.maxJobDelay, delay);

                    if (delay < 0.01) result.jobsOnTime++;
                    else result.jobsDelayed++;
                }
            }

            int totalJobs = result.jobsOnTime + result.jobsDelayed;
            result.averageDelayPerJob = (totalJobs > 0) ?
                    result.totalDelay / totalJobs : 0.0;
        }

        return result;
    }

    /**
     * Export raw data (all individual test results)
     */
    static void exportRawData(
            const std::vector<TestResult>& results,
            const std::string& filename) {

        std::ofstream csv(filename);
        csv << "Instance,Strategy,AnomalyPosition,Machines,Jobs,ARHs,"
            << "ComputationTimeMs,TotalDelay,AvgDelayPerJob,MaxJobDelay,"
            << "JobsOnTime,JobsDelayed,AnomalyTime,CBMStart,CBMDuration\n";

        for (const auto& r : results) {
            csv << r.instanceName << ","
                << r.strategy << ","
                << r.anomalyPosition << ","
                << r.numMachines << ","
                << r.numJobs << ","
                << r.numARHs << ","
                << std::fixed << std::setprecision(3)
                << r.computationTimeMs << ","
                << r.totalDelay << ","
                << r.averageDelayPerJob << ","
                << r.maxJobDelay << ","
                << r.jobsOnTime << ","
                << r.jobsDelayed << ","
                << r.anomalyTime << ","
                << r.cbmStartTime << ","
                << r.cbmDuration << "\n";
        }
        csv.close();
    }

    /**
     * Export aggregated statistics (Table 4.6 format)
     */
    static void exportAggregatedData(
            const std::vector<TestResult>& results,
            const std::string& filename) {

        std::map<std::string, std::vector<TestResult>> grouped;

        for (const auto& r : results) {
            std::string key = std::to_string(r.numMachines) + "M_" +
                    std::to_string(r.numJobs) + "J_" +
                    std::to_string(r.numARHs) + "ARH_" +
                    r.strategy + "_" +
                    r.anomalyPosition;
            grouped[key].push_back(r);
        }

        std::ofstream csv(filename);
        csv << "Configuration,Tests,AvgTimeMs,StdTimeMs,MinTimeMs,MaxTimeMs,"
            << "AvgDelay,StdDelay,MinDelay,MaxDelay,SuccessRate\n";

        for (const auto& [config, tests] : grouped) {
            auto agg = calculateStatistics(tests);
            csv << config << ","
                << agg.totalTests << ","
                << std::fixed << std::setprecision(2)
                << agg.avgComputationTime << ","
                << agg.stdComputationTime << ","
                << agg.minComputationTime << ","
                << agg.maxComputationTime << ","
                << agg.avgTotalDelay << ","
                << agg.stdTotalDelay << ","
                << agg.minTotalDelay << ","
                << agg.maxTotalDelay << ","
                << (agg.successRate * 100.0) << "\n";
        }
        csv.close();
    }

    /**
     * Generate LaTeX tables (Table 4.7 format)
     */
    static void exportLatexTables(
            const std::vector<TestResult>& results,
            const std::string& filename) {

        std::ofstream tex(filename);

        tex << "\\documentclass{article}\n";
        tex << "\\usepackage{booktabs}\n";
        tex << "\\begin{document}\n\n";

        tex << "\\section*{Table 4.6: Computational Performance (Réactivité)}\n";
        tex << "\\begin{tabular}{llrrrrr}\n";
        tex << "\\toprule\n";
        tex << "Config & Strategy & \\multicolumn{2}{c}{Computation Time (ms)} & "
            << "\\multicolumn{2}{c}{Total Delay} & Success \\\\\n";
        tex << "       &          & Mean & Std & Mean & Std & Rate \\\\\n";
        tex << "\\midrule\n";

        std::map<std::string, std::vector<TestResult>> grouped;
        for (const auto& r : results) {
            std::string key = std::to_string(r.numMachines) + "M/" +
                    std::to_string(r.numJobs) + "J";
            grouped[key].push_back(r);
        }

        for (const auto& [config, tests] : grouped) {
            auto agg = calculateStatistics(tests);
            tex << config << " & " << agg.configuration << " & "
                << std::fixed << std::setprecision(2)
                << agg.avgComputationTime << " & "
                << agg.stdComputationTime << " & "
                << agg.avgTotalDelay << " & "
                << agg.stdTotalDelay << " & "
                << (agg.successRate * 100.0) << "\\% \\\\\n";
        }

        tex << "\\bottomrule\n";
        tex << "\\end{tabular}\n\n";
        tex << "\\end{document}\n";
        tex.close();
    }

    /**
     * Generate detailed analysis report
     */
    static void generateAnalysisReport(
            const std::vector<TestResult>& results,
            const std::string& filename) {

        std::ofstream report(filename);

        report << "═══════════════════════════════════════════════════════════\n";
        report << "     BENCHMARK ANALYSIS REPORT\n";
        report << "═══════════════════════════════════════════════════════════\n\n";

        // Overall statistics
        double totalTimeMs = 0, totalDelay = 0;
        for (const auto& r : results) {
            totalTimeMs += r.computationTimeMs;
            totalDelay += r.totalDelay;
        }

        report << "OVERALL STATISTICS:\n";
        report << "  Total tests executed: " << results.size() << "\n";
        report << "  Average computation time: " << (totalTimeMs / results.size()) << " ms\n";
        report << "  Average total delay: " << (totalDelay / results.size()) << "\n\n";

        // Strategy comparison
        report << "STRATEGY COMPARISON:\n";
        std::map<std::string, std::vector<TestResult>> byStrategy;
        for (const auto& r : results) {
            byStrategy[r.strategy].push_back(r);
        }

        for (const auto& [strategy, tests] : byStrategy) {
            auto agg = calculateStatistics(tests);
            report << "  " << strategy << ":\n";
            report << "    Avg computation time: " << agg.avgComputationTime << " ms\n";
            report << "    Avg delay: " << agg.avgTotalDelay << "\n";
            report << "    Success rate: " << (agg.successRate * 100.0) << "%\n\n";
        }

        // Anomaly position impact
        report << "ANOMALY POSITION IMPACT:\n";
        std::map<std::string, std::vector<TestResult>> byPosition;
        for (const auto& r : results) {
            byPosition[r.anomalyPosition].push_back(r);
        }

        for (const auto& [position, tests] : byPosition) {
            auto agg = calculateStatistics(tests);
            report << "  " << position << ":\n";
            report << "    Avg delay: " << agg.avgTotalDelay << "\n";
            report << "    Success rate: " << (agg.successRate * 100.0) << "%\n\n";
        }

        // ARH resource impact
        report << "ARH RESOURCE IMPACT:\n";
        std::map<int, std::vector<TestResult>> byARH;
        for (const auto& r : results) {
            byARH[r.numARHs].push_back(r);
        }

        for (const auto& [numARH, tests] : byARH) {
            auto agg = calculateStatistics(tests);
            report << "  " << numARH << " technicians:\n";
            report << "    Avg computation time: " << agg.avgComputationTime << " ms\n";
            report << "    Avg delay: " << agg.avgTotalDelay << "\n\n";
        }

        report << "═══════════════════════════════════════════════════════════\n";
        report.close();
    }

    /**
     * Calculate statistics for a group of test results
     */
    static AggregatedResults calculateStatistics(
            const std::vector<TestResult>& tests) {

        AggregatedResults agg;
        agg.totalTests = tests.size();

        if (tests.empty()) return agg;

        // Extract values
        std::vector<double> times, delays;
        int totalJobsOnTime = 0, totalJobs = 0;

        for (const auto& t : tests) {
            times.push_back(t.computationTimeMs);
            delays.push_back(t.totalDelay);
            totalJobsOnTime += t.jobsOnTime;
            totalJobs += (t.jobsOnTime + t.jobsDelayed);
        }

        // Computation time statistics
        agg.avgComputationTime = mean(times);
        agg.stdComputationTime = stddev(times, agg.avgComputationTime);
        agg.minComputationTime = *std::min_element(times.begin(), times.end());
        agg.maxComputationTime = *std::max_element(times.begin(), times.end());

        // Delay statistics
        agg.avgTotalDelay = mean(delays);
        agg.stdTotalDelay = stddev(delays, agg.avgTotalDelay);
        agg.minTotalDelay = *std::min_element(delays.begin(), delays.end());
        agg.maxTotalDelay = *std::max_element(delays.begin(), delays.end());

        // Success rate
        agg.successRate = (totalJobs > 0) ?
                (double)totalJobsOnTime / totalJobs : 0.0;

        return agg;
    }

    static double mean(const std::vector<double>& values) {
        double sum = 0.0;
        for (double v : values) sum += v;
        return sum / values.size();
    }

    static double stddev(const std::vector<double>& values, double mean) {
        double sumSquaredDiff = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSquaredDiff += diff * diff;
        }
        return std::sqrt(sumSquaredDiff / values.size());
    }
};

constexpr BenchmarkRunner::AnomalyPosition BenchmarkRunner::POSITIONS[3];