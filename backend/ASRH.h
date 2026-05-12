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
        DiagnosticResult diag,
        std::string strategy)
    {
        std::cout << "\n************************************************************\n";
        std::cout << "[ASRH] Broadcasting CFP to " << registeredARHs.size()
                  << " ARH agents...\n";
        std::cout << "************************************************************\n";

        double deadlineLimit = 0.0;
        if (strategy == "SOM") {
            deadlineLimit = diag.estimatedRUL.min;
        } else if (strategy == "SOP") {
            deadlineLimit = diag.estimatedRUL.max;
        } else {
            deadlineLimit = diag.estimatedRUL.prob; // default
        }

        std::vector<CBMProposal> proposals;
        for (const auto& arh : registeredARHs) {
            // Check competence
            bool hasCompetence = false;
            for (const auto& comp : arh.competencies) {
                if (comp == diag.requiredCompetence) {
                    hasCompetence = true;
                    break;
                }
            }
            if (!hasCompetence && !diag.requiredCompetence.empty()) {
                std::cout << "[ASRH] REJECTED " << arh.id 
                          << ": Lacks required competence '" << diag.requiredCompetence << "'.\n";
                continue;
            }

            auto props = arh.propose();
            for (auto& prop : props) {
                double expectedFinish = prop.cbmStart + prop.cbmDuration.prob;
                if (expectedFinish <= deadlineLimit) {
                    proposals.push_back(prop);
                } else {
                    std::cout << "[ASRH] REJECTED " << arh.id 
                              << " window [" << prop.cbmStart << "]: Expected finish (" << expectedFinish 
                              << ") exceeds RUL deadline limit (" << deadlineLimit 
                              << ") under strategy " << strategy << ".\n";
                }
            }
        }

        std::cout << "\n[ASRH] Received " << proposals.size()
                  << " valid proposal(s).\n";
        return proposals;
    }
};
