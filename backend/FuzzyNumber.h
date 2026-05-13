#pragma once
#include <string>
#include <sstream>
#include <algorithm>

class FuzzyNumber {
public:
    double min, prob, max;

    FuzzyNumber(double m = 0, double p = 0, double mx = 0) : min(m), prob(p), max(mx) {}

    FuzzyNumber operator+(const FuzzyNumber& other) const {
        return FuzzyNumber(min + other.min, prob + other.prob, max + other.max);
    }

    FuzzyNumber operator-(double val) const {
        return FuzzyNumber(min - val, prob - val, max - val);
    }

    FuzzyNumber operator/(int divisor) const {
        if (divisor == 0) return FuzzyNumber(0, 0, 0);
        return FuzzyNumber(min / divisor, prob / divisor, max / divisor);
    }

    FuzzyNumber operator*(double val) const {
        return FuzzyNumber(min * val, prob * val, max * val);
    }

    FuzzyNumber maxOfZero() const {
        return FuzzyNumber(std::max(0.0, min), std::max(0.0, prob), std::max(0.0, max));
    }

    std::string str() const {
        std::ostringstream ss;
        ss << "[" << min << ", " << prob << ", " << max << "]";
        return ss.str();
    }
};
