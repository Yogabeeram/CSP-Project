package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.dto.BloomDistributionReport;
import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.exception.PaperGenerationException;
import com.college.questionpapergenerator.util.PaperSelectionUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class BloomDistributionService {

    /**
     * Converts percentages to integer counts using largest remainder; ties follow Bloom enum order.
     */
    public Map<BloomLevel, Integer> calculateRequiredCounts(
            Map<BloomLevel, BigDecimal> percentages,
            int totalQuestions,
            List<Question> availableQuestions) {
        validatePercentages(percentages);

        Map<BloomLevel, Integer> requiredCounts = PaperSelectionUtil.allocatePercentages(
                percentages,
                List.of(BloomLevel.values()),
                totalQuestions
        );
        Map<BloomLevel, Long> availableCounts = availableQuestions.stream()
                .filter(Question::isApproved)
                .filter(question -> question.getBloomLevel() != null && question.getDifficulty() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        Question::getBloomLevel,
                        () -> new EnumMap<>(BloomLevel.class),
                        java.util.stream.Collectors.counting()
                ));

        for (BloomLevel level : BloomLevel.values()) {
            int required = requiredCounts.getOrDefault(level, 0);
            long available = availableCounts.getOrDefault(level, 0L);
            if (available < required) {
                throw new PaperGenerationException(
                        "Insufficient approved classified questions for Bloom level " + level
                                + ": need " + required + ", found " + available);
            }
        }
        return requiredCounts;
    }

    public BloomDistributionReport validateAndReport(
            Map<BloomLevel, BigDecimal> percentages,
            Map<BloomLevel, Integer> requiredCounts,
            List<Question> selectedQuestions) {
        validatePercentages(percentages);
        Map<BloomLevel, Integer> actualCounts = new EnumMap<>(BloomLevel.class);
        for (BloomLevel level : BloomLevel.values()) {
            actualCounts.put(level, 0);
        }
        for (Question question : selectedQuestions) {
            if (!question.isApproved() || question.getBloomLevel() == null) {
                throw new PaperGenerationException(
                        "Generated paper contains an unapproved or unclassified question");
            }
            actualCounts.merge(question.getBloomLevel(), 1, Integer::sum);
        }
        for (BloomLevel level : BloomLevel.values()) {
            if (!requiredCounts.getOrDefault(level, 0).equals(actualCounts.get(level))) {
                throw new PaperGenerationException(
                        "Generated paper does not satisfy the requested Bloom distribution for " + level);
            }
        }
        int selectedCount = selectedQuestions.size();
        if (selectedCount == 0) {
            throw new PaperGenerationException("Generated paper contains no questions");
        }
        Map<BloomLevel, BigDecimal> actualPercentages = new EnumMap<>(BloomLevel.class);
        for (BloomLevel level : BloomLevel.values()) {
            BigDecimal actualPercentage = BigDecimal.valueOf(actualCounts.get(level))
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(selectedCount), 2, RoundingMode.HALF_UP);
            actualPercentages.put(level, actualPercentage);
        }
        return new BloomDistributionReport(percentages, requiredCounts, actualCounts, actualPercentages);
    }

    private void validatePercentages(Map<BloomLevel, BigDecimal> percentages) {
        if (percentages == null || percentages.size() != BloomLevel.values().length) {
            throw new PaperGenerationException(
                    "Specify a percentage for every Bloom level: "
                            + String.join(", ", java.util.Arrays.stream(BloomLevel.values())
                            .map(Enum::name)
                            .toList()));
        }
        for (BloomLevel level : BloomLevel.values()) {
            if (!percentages.containsKey(level) || percentages.get(level) == null) {
                throw new PaperGenerationException("Missing percentage for Bloom level " + level);
            }
        }
        PaperSelectionUtil.allocatePercentages(
                percentages,
                List.of(BloomLevel.values()),
                100
        );
    }
}
