package com.college.questionpapergenerator.dto;

import com.college.questionpapergenerator.entity.BloomLevel;

import java.math.BigDecimal;
import java.util.Map;

public record BloomDistributionReport(
        Map<BloomLevel, BigDecimal> percentages,
        Map<BloomLevel, Integer> requiredCounts,
        Map<BloomLevel, Integer> actualCounts,
        Map<BloomLevel, BigDecimal> actualPercentages
) {
    public BloomDistributionReport {
        percentages = Map.copyOf(percentages);
        requiredCounts = Map.copyOf(requiredCounts);
        actualCounts = Map.copyOf(actualCounts);
        actualPercentages = Map.copyOf(actualPercentages);
    }
}
