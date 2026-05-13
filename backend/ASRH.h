#pragma once
#include "ARH.h"
#include <vector>
#include "JsonLogger.h"

class ASRH {
public:
    std::vector<ARH> registeredARHs;

    explicit ASRH(const std::vector<ARH>& arhs) : registeredARHs(arhs) {}

    std::vector<CBMProposal> callForProposals(DiagnosticResult diag, std::string strategy, double alertTime) {
        std::string skill = diag.requiredCompetence.empty() ? "Mechanical" : diag.requiredCompetence;
        jsonLog("ASRH", "Scanning for technicians with skill: " + skill, "info", 2);

        // Pre-filtering based on Competence
        std::vector<ARH> qualifiedARHs;
        for (const auto& arh : registeredARHs) {
            for (const auto& comp : arh.competencies) {
                if (comp == skill) {
                    qualifiedARHs.push_back(arh);
                    break;
                }
            }
        }

        double deadlineLimit = (strategy == "SOM") ? diag.estimatedRUL.min : 
                               (strategy == "SOP") ? diag.estimatedRUL.max : diag.estimatedRUL.prob;

        // Reception & Validation
        std::vector<CBMProposal> proposals;
        std::string respondingARHs = "";
        
        for (const auto& arh : qualifiedARHs) {
            auto props = arh.propose(alertTime); 
            for (auto& prop : props) {
                double expectedFinish = prop.cbmStart + prop.cbmDuration.prob;
                if (expectedFinish <= deadlineLimit) {
                    proposals.push_back(prop);
                    if (!respondingARHs.empty()) respondingARHs += " and ";
                    respondingARHs += arh.id;
                } else {
                    jsonLog("ASRH", "REJECTED " + arh.id + ": Expected finish (" + std::to_string((int)expectedFinish) + ") exceeds RUL deadline limit (" + std::to_string((int)deadlineLimit) + ").", "warn");
                }
            }
        }

        if (respondingARHs.empty()) jsonLog("ASRH", "No valid proposals received.", "error");
        else jsonLog("ASRH", "Received valid proposals from " + respondingARHs + ". Forwarding to AMS.", "info");
        
        return proposals;
    }

    void confirm(const std::string& arhId) {
        jsonLog("ASRH", "Confirmation sent to " + arhId + ". Rejecting other proposals.", "info", 7);
    }
};