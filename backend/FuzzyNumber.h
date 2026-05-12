/**
 * @file FuzzyNumber.h
 * @brief Triangular Fuzzy Number (TFN) for uncertain time durations.
 */
#pragma once
#include <algorithm>
#include <iostream>
#include <sstream>

struct FuzzyNumber {
    double min, prob, max;
    FuzzyNumber(double m=0, double p=0, double mx=0) : min(m), prob(p), max(mx) {}

    FuzzyNumber operator+(const FuzzyNumber& o) const { return {min+o.min, prob+o.prob, max+o.max}; }
    FuzzyNumber operator+(double v) const { return {min+v, prob+v, max+v}; }
    FuzzyNumber operator-(double v) const { return {min-v, prob-v, max-v}; }
    FuzzyNumber operator*(double w) const { return {min*w, prob*w, max*w}; }
    FuzzyNumber operator/(double d) const { return {min/d, prob/d, max/d}; }

    FuzzyNumber maxOfZero() const {
        return {std::max(0.0,min), std::max(0.0,prob), std::max(0.0,max)};
    }
    FuzzyNumber maxWith(double v) const {
        return {std::max(v,min), std::max(v,prob), std::max(v,max)};
    }
    std::string str() const {
        std::ostringstream o;
        if(min==prob && prob==max) o<<prob;
        else o<<"("<<min<<", "<<prob<<", "<<max<<")";
        return o.str();
    }
};

inline std::ostream& operator<<(std::ostream& os, const FuzzyNumber& f) {
    return os << f.str();
}
