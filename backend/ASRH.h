/**
 * @file ASRH.h
 * @brief Agent Superviseur des Ressources Humaines — HR supervisor.
 */
#pragma once
#include "ARH.h"
#include <vector>
#include <iostream>

class ASRH {
public:
    std::vector<ARH> registeredARHs;

    explicit ASRH(const std::vector<ARH>& arhs) : registeredARHs(arhs) {}

    std::vector<CBMProposal> callForProposals(
        double startTime,
        const std::vector<ProductionJob>& jobs,
        const std::vector<TBMBlock>& tbmBlocks)
    {
        std::cout << "\n************************************************************\n";
        std::cout << "[ASRH] Broadcasting CFP to " << registeredARHs.size()
                  << " ARH agents...\n";
        std::cout << "************************************************************\n";

        std::vector<CBMProposal> proposals;
        for (const auto& arh : registeredARHs) {
            auto prop = arh.propose(startTime, jobs, tbmBlocks);
            if (prop) proposals.push_back(*prop);
        }

        std::cout << "\n[ASRH] Received " << proposals.size()
                  << " valid proposal(s).\n";
        return proposals;
    }
};
