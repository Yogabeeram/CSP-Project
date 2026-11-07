package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.exception.PaperGenerationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BloomDistributionServiceTest {

    private final BloomDistributionService service = new BloomDistributionService();

    @Test
    void allocatesRequestedDistributionForAllSixLevels() {
        Map<BloomLevel, Integer> counts = service.calculateRequiredCounts(
                percentages(20, 20, 20, 20, 10, 10),
                10,
                questionsForAllLevels(2)
        );

        assertEquals(2, counts.get(BloomLevel.REMEMBER));
        assertEquals(2, counts.get(BloomLevel.UNDERSTAND));
        assertEquals(2, counts.get(BloomLevel.APPLY));
        assertEquals(2, counts.get(BloomLevel.ANALYZE));
        assertEquals(1, counts.get(BloomLevel.EVALUATE));
        assertEquals(1, counts.get(BloomLevel.CREATE));
        assertEquals(10, counts.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void roundsFractionalCountsUsingLargestRemainder() {
        Map<BloomLevel, Integer> counts = service.calculateRequiredCounts(
                percentages(16, 16, 17, 17, 17, 17),
                3,
                questionsForAllLevels(3)
        );

        assertEquals(3, counts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(1, counts.get(BloomLevel.APPLY));
        assertEquals(1, counts.get(BloomLevel.ANALYZE));
        assertEquals(1, counts.get(BloomLevel.EVALUATE));
    }

    @Test
    void requiresAnExplicitPercentageForEveryBloomLevel() {
        Map<BloomLevel, BigDecimal> incomplete = new EnumMap<>(BloomLevel.class);
        incomplete.putAll(percentages(20, 20, 20, 20, 10, 10));
        incomplete.remove(BloomLevel.CREATE);

        assertThrows(PaperGenerationException.class, () -> service.calculateRequiredCounts(
                incomplete, 10, questionsForAllLevels(2)));
    }

    @Test
    void rejectsPercentagesThatDoNotTotalOneHundred() {
        assertThrows(PaperGenerationException.class, () -> service.calculateRequiredCounts(
                percentages(20, 20, 20, 20, 10, 9),
                10,
                questionsForAllLevels(2)
        ));
    }

    @Test
    void reportsWhenApprovedClassifiedBankCannotSatisfyLevelCount() {
        List<Question> available = new ArrayList<>();
        available.addAll(questions(BloomLevel.REMEMBER, 2));
        available.addAll(questions(BloomLevel.UNDERSTAND, 2));
        available.addAll(questions(BloomLevel.APPLY, 2));
        available.addAll(questions(BloomLevel.ANALYZE, 2));
        available.addAll(questions(BloomLevel.EVALUATE, 1));

        assertThrows(PaperGenerationException.class, () -> service.calculateRequiredCounts(
                percentages(20, 20, 20, 20, 10, 10),
                10,
                available
        ));
    }

    @Test
    void doesNotCountUnapprovedQuestionsAsAvailable() {
        List<Question> available = questionsForAllLevels(1);
        available.removeIf(question -> question.getBloomLevel() == BloomLevel.CREATE);
        Question unapprovedCreate = question(BloomLevel.CREATE);
        unapprovedCreate.setApproved(false);
        available.add(unapprovedCreate);

        assertThrows(PaperGenerationException.class, () -> service.calculateRequiredCounts(
                percentages(0, 0, 0, 0, 0, 100),
                1,
                available
        ));
    }

    @Test
    void validatesAndReportsActualDistribution() {
        Map<BloomLevel, BigDecimal> percentages = percentages(50, 50, 0, 0, 0, 0);
        Map<BloomLevel, Integer> required = service.calculateRequiredCounts(
                percentages,
                2,
                questionsForAllLevels(1)
        );

        var report = service.validateAndReport(
                percentages,
                required,
                List.of(question(BloomLevel.REMEMBER), question(BloomLevel.UNDERSTAND))
        );

        assertEquals(1, report.actualCounts().get(BloomLevel.REMEMBER));
        assertEquals(1, report.actualCounts().get(BloomLevel.UNDERSTAND));
        assertEquals(2, report.actualCounts().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(new BigDecimal("50.00"), report.actualPercentages().get(BloomLevel.REMEMBER));
        assertEquals(new BigDecimal("50.00"), report.actualPercentages().get(BloomLevel.UNDERSTAND));
    }

    @Test
    void rejectsGeneratedPaperWithIncorrectActualDistribution() {
        Map<BloomLevel, BigDecimal> percentages = percentages(50, 50, 0, 0, 0, 0);
        Map<BloomLevel, Integer> required = service.calculateRequiredCounts(
                percentages,
                2,
                questionsForAllLevels(1)
        );

        assertThrows(PaperGenerationException.class, () -> service.validateAndReport(
                percentages,
                required,
                List.of(question(BloomLevel.REMEMBER), question(BloomLevel.REMEMBER))
        ));
    }

    private Map<BloomLevel, BigDecimal> percentages(
            int remember,
            int understand,
            int apply,
            int analyze,
            int evaluate,
            int create) {
        Map<BloomLevel, BigDecimal> values = new EnumMap<>(BloomLevel.class);
        values.put(BloomLevel.REMEMBER, BigDecimal.valueOf(remember));
        values.put(BloomLevel.UNDERSTAND, BigDecimal.valueOf(understand));
        values.put(BloomLevel.APPLY, BigDecimal.valueOf(apply));
        values.put(BloomLevel.ANALYZE, BigDecimal.valueOf(analyze));
        values.put(BloomLevel.EVALUATE, BigDecimal.valueOf(evaluate));
        values.put(BloomLevel.CREATE, BigDecimal.valueOf(create));
        return values;
    }

    private List<Question> questionsForAllLevels(int countEach) {
        List<Question> questions = new ArrayList<>();
        for (BloomLevel level : BloomLevel.values()) {
            questions.addAll(questions(level, countEach));
        }
        return questions;
    }

    private List<Question> questions(BloomLevel bloomLevel, int count) {
        List<Question> questions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            questions.add(question(bloomLevel));
        }
        return questions;
    }

    private Question question(BloomLevel bloomLevel) {
        Subject subject = new Subject("Computer Science", "CS");
        subject.setId(1L);
        Topic topic = new Topic("Algorithms", 1, subject);
        topic.setId(1L);
        return new Question("Question", subject, topic, 5, Difficulty.MEDIUM, bloomLevel, true);
    }
}
