package com.college.questionpapergenerator.util;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.exception.PaperGenerationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PaperSelectionUtil {

    private static final long MAX_SEARCH_NODES = 1_000_000;

    private PaperSelectionUtil() {
    }

    public static <K> Map<K, Integer> allocatePercentages(
            Map<K, BigDecimal> percentages,
            List<K> categoryOrder,
            int total) {
        if (percentages == null || percentages.isEmpty()) {
            throw new PaperGenerationException("Distribution percentages are required");
        }

        BigDecimal sum = BigDecimal.ZERO;
        Map<K, BigDecimal> normalized = new HashMap<>();
        for (Map.Entry<K, BigDecimal> entry : percentages.entrySet()) {
            BigDecimal percentage = entry.getValue();
            if (!categoryOrder.contains(entry.getKey()) || percentage == null
                    || percentage.compareTo(BigDecimal.ZERO) < 0
                    || percentage.compareTo(new BigDecimal("100")) > 0) {
                throw new PaperGenerationException("Distribution contains an invalid category or percentage");
            }
            normalized.put(entry.getKey(), percentage);
            sum = sum.add(percentage);
        }
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            throw new PaperGenerationException("Distribution percentages must add up to exactly 100");
        }

        Map<K, Integer> allocations = new LinkedHashMap<>();
        Map<K, BigDecimal> remainders = new HashMap<>();
        int allocated = 0;
        for (K category : categoryOrder) {
            BigDecimal raw = normalized.getOrDefault(category, BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(total))
                    .divide(new BigDecimal("100"));
            int base = raw.setScale(0, RoundingMode.DOWN).intValueExact();
            allocations.put(category, base);
            remainders.put(category, raw.subtract(BigDecimal.valueOf(base)));
            allocated += base;
        }

        List<K> remainderOrder = new ArrayList<>(categoryOrder);
        remainderOrder.sort(Comparator
                .<K, BigDecimal>comparing(remainders::get)
                .reversed()
                .thenComparingInt(categoryOrder::indexOf));
        for (int index = 0; index < total - allocated; index++) {
            K category = remainderOrder.get(index);
            allocations.put(category, allocations.get(category) + 1);
        }
        return allocations;
    }

    public static List<Selection> select(
            List<Question> approvedCandidates,
            int totalMarks,
            int totalQuestions,
            Map<Difficulty, Integer> difficultyTargets,
            Map<BloomLevel, Integer> bloomTargets,
            Map<Integer, Integer> unitMarkTargets,
            List<Slot> slots) {
        if (approvedCandidates.size() < totalQuestions) {
            throw new PaperGenerationException(
                    "Insufficient approved questions: need " + totalQuestions
                            + ", found " + approvedCandidates.size());
        }
        if (difficultyTargets.values().stream().mapToInt(Integer::intValue).sum() != totalQuestions
                || bloomTargets.values().stream().mapToInt(Integer::intValue).sum() != totalQuestions) {
            throw new PaperGenerationException("Difficulty and Bloom targets must each total the question count");
        }
        if (slots != null && !slots.isEmpty() && slots.size() != totalQuestions) {
            throw new PaperGenerationException("Section question slots must equal the total question count");
        }

        List<Question> candidates = approvedCandidates.stream()
                .filter(Question::isApproved)
                .toList();
        if (candidates.size() < totalQuestions) {
            throw new PaperGenerationException(
                    "Insufficient approved questions: need " + totalQuestions
                            + ", found " + candidates.size());
        }
        for (Question candidate : candidates) {
            if (candidate.getId() == null || candidate.getMarks() == null || candidate.getMarks() <= 0
                    || candidate.getDifficulty() == null || candidate.getBloomLevel() == null
                    || candidate.getTopic() == null || candidate.getTopic().getUnitNumber() == null) {
                throw new PaperGenerationException("Question bank contains an incomplete approved question");
            }
        }
        candidates = candidates.stream()
                .sorted(Comparator.comparing(Question::getId))
                .toList();

        Search search = new Search(candidates, totalMarks, totalQuestions, difficultyTargets,
                bloomTargets, unitMarkTargets == null ? Map.of() : unitMarkTargets,
                slots == null ? List.of() : slots);
        if (!search.solve()) {
            if (search.limitReached) {
                throw new PaperGenerationException(
                        "Constraint search limit reached; narrow the selected units or distributions");
            }
            throw new PaperGenerationException(
                    "No selection satisfies all required marks, question count, difficulty, Bloom, "
                            + "unit-weightage, and section constraints");
        }
        return List.copyOf(search.selected);
    }

    public record Slot(String sectionName, Integer requiredMarks) {
    }

    public record Selection(Question question, String sectionName) {
    }

    private static final class Search {

        private final List<Question> candidates;
        private final int totalMarks;
        private final int totalQuestions;
        private final Map<Difficulty, Integer> difficultyTargets;
        private final Map<BloomLevel, Integer> bloomTargets;
        private final Map<Integer, Integer> unitMarkTargets;
        private final List<Slot> slots;
        private final boolean orderedSlots;
        private final List<Selection> selected = new ArrayList<>();
        private final Set<Long> selectedIds = new HashSet<>();
        private final Map<Difficulty, Integer> difficultyCounts = new HashMap<>();
        private final Map<BloomLevel, Integer> bloomCounts = new HashMap<>();
        private final Map<Integer, Integer> unitMarks = new HashMap<>();
        private long visitedNodes;
        private boolean limitReached;
        private int currentMarks;

        private Search(
                List<Question> candidates,
                int totalMarks,
                int totalQuestions,
                Map<Difficulty, Integer> difficultyTargets,
                Map<BloomLevel, Integer> bloomTargets,
                Map<Integer, Integer> unitMarkTargets,
                List<Slot> slots) {
            this.candidates = candidates;
            this.totalMarks = totalMarks;
            this.totalQuestions = totalQuestions;
            this.difficultyTargets = difficultyTargets;
            this.bloomTargets = bloomTargets;
            this.unitMarkTargets = unitMarkTargets;
            this.slots = slots.isEmpty()
                    ? java.util.Collections.nCopies(totalQuestions, new Slot("Section A", null))
                    : slots;
            this.orderedSlots = slots.isEmpty();
        }

        private boolean solve() {
            return search(0, 0);
        }

        private boolean search(int slotIndex, int startIndex) {
            if (++visitedNodes > MAX_SEARCH_NODES) {
                limitReached = true;
                return false;
            }
            if (slotIndex == totalQuestions) {
                return currentMarks == totalMarks
                        && distributionMatches(difficultyTargets, difficultyCounts)
                        && distributionMatches(bloomTargets, bloomCounts)
                        && unitWeightageMatches();
            }
            if (!canStillMeetCounts(totalQuestions - slotIndex)) {
                return false;
            }

            Slot slot = slots.get(slotIndex);
            int firstCandidate = orderedSlots ? startIndex : 0;
            for (int candidateIndex = firstCandidate; candidateIndex < candidates.size(); candidateIndex++) {
                Question question = candidates.get(candidateIndex);
                int marks = question.getMarks();
                if (selectedIds.contains(question.getId())
                        || (slot.requiredMarks() != null && marks != slot.requiredMarks())
                        || marks > totalMarks - currentMarks
                        || exceedsTarget(difficultyTargets, difficultyCounts, question.getDifficulty())
                        || exceedsTarget(bloomTargets, bloomCounts, question.getBloomLevel())
                        || exceedsUnitMarkTarget(question)) {
                    continue;
                }

                add(question, slot);
                if (search(slotIndex + 1, orderedSlots ? candidateIndex + 1 : 0)) {
                    return true;
                }
                remove(question);
                if (limitReached) {
                    return false;
                }
            }
            return false;
        }

        private boolean canStillMeetCounts(int remainingSlots) {
            for (Difficulty difficulty : Difficulty.values()) {
                int needed = difficultyTargets.getOrDefault(difficulty, 0)
                        - difficultyCounts.getOrDefault(difficulty, 0);
                if (needed < 0 || needed > remainingSlots) {
                    return false;
                }
            }
            for (BloomLevel bloomLevel : BloomLevel.values()) {
                int needed = bloomTargets.getOrDefault(bloomLevel, 0)
                        - bloomCounts.getOrDefault(bloomLevel, 0);
                if (needed < 0 || needed > remainingSlots) {
                    return false;
                }
            }
            return true;
        }

        private <K> boolean exceedsTarget(Map<K, Integer> targets, Map<K, Integer> counts, K category) {
            return counts.getOrDefault(category, 0) + 1 > targets.getOrDefault(category, 0);
        }

        private boolean exceedsUnitMarkTarget(Question question) {
            if (unitMarkTargets.isEmpty()) {
                return false;
            }
            int unit = question.getTopic().getUnitNumber();
            Integer target = unitMarkTargets.get(unit);
            return target == null
                    || question.getMarks() > target - unitMarks.getOrDefault(unit, 0);
        }

        private void add(Question question, Slot slot) {
            selectedIds.add(question.getId());
            selected.add(new Selection(question, slot.sectionName()));
            currentMarks += question.getMarks();
            difficultyCounts.merge(question.getDifficulty(), 1, Integer::sum);
            bloomCounts.merge(question.getBloomLevel(), 1, Integer::sum);
            unitMarks.merge(question.getTopic().getUnitNumber(), question.getMarks(), Integer::sum);
        }

        private void remove(Question question) {
            selectedIds.remove(question.getId());
            selected.remove(selected.size() - 1);
            currentMarks -= question.getMarks();
            decrement(difficultyCounts, question.getDifficulty(), 1);
            decrement(bloomCounts, question.getBloomLevel(), 1);
            decrement(unitMarks, question.getTopic().getUnitNumber(), question.getMarks());
        }

        private <K> void decrement(Map<K, Integer> counts, K key, int amount) {
            int remaining = counts.getOrDefault(key, 0) - amount;
            if (remaining == 0) {
                counts.remove(key);
            } else {
                counts.put(key, remaining);
            }
        }

        private <K> boolean distributionMatches(Map<K, Integer> targets, Map<K, Integer> counts) {
            for (Map.Entry<K, Integer> target : targets.entrySet()) {
                if (!target.getValue().equals(counts.getOrDefault(target.getKey(), 0))) {
                    return false;
                }
            }
            return true;
        }

        private boolean unitWeightageMatches() {
            if (unitMarkTargets.isEmpty()) {
                return true;
            }
            for (Map.Entry<Integer, Integer> target : unitMarkTargets.entrySet()) {
                if (!target.getValue().equals(unitMarks.getOrDefault(target.getKey(), 0))) {
                    return false;
                }
            }
            return true;
        }
    }
}
