package com.college.questionpapergenerator.util;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.exception.PaperGenerationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaperSelectionUtilTest {

    @Test
    void selectsQuestionsMeetingEveryExactConstraint() {
        List<Question> candidates = standardQuestions();

        List<PaperSelectionUtil.Selection> selection = PaperSelectionUtil.select(
                candidates,
                12,
                4,
                difficultyTargets(2, 1, 1),
                bloomTargets(1, 1, 1, 1, 0, 0),
                Map.of(1, 12),
                List.of(new PaperSelectionUtil.Slot("Section A", 3),
                        new PaperSelectionUtil.Slot("Section A", 3),
                        new PaperSelectionUtil.Slot("Section B", 3),
                        new PaperSelectionUtil.Slot("Section B", 3))
        );

        assertEquals(4, selection.size());
        assertEquals(12, selection.stream().mapToInt(s -> s.question().getMarks()).sum());
        assertEquals(4, selection.stream().map(s -> s.question().getId()).distinct().count());
    }

    @Test
    void reportsInsufficientApprovedQuestions() {
        assertThrows(PaperGenerationException.class, () -> PaperSelectionUtil.select(
                standardQuestions().subList(0, 1),
                6,
                2,
                difficultyTargets(2, 0, 0),
                bloomTargets(2, 0, 0, 0, 0, 0),
                Map.of(),
                List.of()
        ));
    }

    @Test
    void preventsSelectingTheSameQuestionTwice() {
        Question onlyQuestion = standardQuestions().get(0);
        assertThrows(PaperGenerationException.class, () -> PaperSelectionUtil.select(
                List.of(onlyQuestion, onlyQuestion),
                6,
                2,
                difficultyTargets(2, 0, 0),
                bloomTargets(2, 0, 0, 0, 0, 0),
                Map.of(),
                List.of()
        ));
    }

    @Test
    void rejectsIncorrectMarksTotal() {
        assertThrows(PaperGenerationException.class, () -> PaperSelectionUtil.select(
                standardQuestions(),
                11,
                4,
                difficultyTargets(2, 1, 1),
                bloomTargets(1, 1, 1, 1, 0, 0),
                Map.of(),
                List.of()
        ));
    }

    @Test
    void enforcesDifficultyDistribution() {
        assertThrows(PaperGenerationException.class, () -> PaperSelectionUtil.select(
                standardQuestions(),
                12,
                4,
                difficultyTargets(1, 2, 1),
                bloomTargets(1, 1, 1, 1, 0, 0),
                Map.of(),
                List.of()
        ));
    }

    @Test
    void enforcesBloomDistribution() {
        assertThrows(PaperGenerationException.class, () -> PaperSelectionUtil.select(
                standardQuestions(),
                12,
                4,
                difficultyTargets(2, 1, 1),
                bloomTargets(2, 0, 1, 1, 0, 0),
                Map.of(),
                List.of()
        ));
    }

    @Test
    void reportsConflictingDifficultyAndBloomConstraints() {
        List<Question> candidates = List.of(
                question(1, Difficulty.EASY, BloomLevel.ANALYZE),
                question(2, Difficulty.MEDIUM, BloomLevel.REMEMBER)
        );
        assertThrows(PaperGenerationException.class, () -> PaperSelectionUtil.select(
                candidates,
                6,
                2,
                difficultyTargets(2, 0, 0),
                bloomTargets(2, 0, 0, 0, 0, 0),
                Map.of(),
                List.of()
        ));
    }

    @Test
    void distributesRoundingRemaindersByLargestFractionThenEnumOrder() {
        Map<Difficulty, Integer> allocation = PaperSelectionUtil.allocatePercentages(
                Map.of(
                        Difficulty.EASY, new java.math.BigDecimal("33.33"),
                        Difficulty.MEDIUM, new java.math.BigDecimal("33.33"),
                        Difficulty.HARD, new java.math.BigDecimal("33.34")),
                List.of(Difficulty.values()),
                2
        );

        assertEquals(2, allocation.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(1, allocation.get(Difficulty.HARD));
        assertEquals(1, allocation.get(Difficulty.EASY));
        assertEquals(0, allocation.get(Difficulty.MEDIUM));
    }

    private List<Question> standardQuestions() {
        return new ArrayList<>(List.of(
                question(1, Difficulty.EASY, BloomLevel.REMEMBER),
                question(2, Difficulty.EASY, BloomLevel.UNDERSTAND),
                question(3, Difficulty.MEDIUM, BloomLevel.APPLY),
                question(4, Difficulty.HARD, BloomLevel.ANALYZE)
        ));
    }

    private Question question(long id, Difficulty difficulty, BloomLevel bloomLevel) {
        Subject subject = new Subject("Computer Science", "CS");
        subject.setId(1L);
        Topic topic = new Topic("Algorithms", 1, subject);
        topic.setId(1L);
        Question question = new Question(
                "Question " + id, subject, topic, 3, difficulty, bloomLevel, true);
        question.setId(id);
        return question;
    }

    private Map<Difficulty, Integer> difficultyTargets(int easy, int medium, int hard) {
        Map<Difficulty, Integer> targets = new EnumMap<>(Difficulty.class);
        targets.put(Difficulty.EASY, easy);
        targets.put(Difficulty.MEDIUM, medium);
        targets.put(Difficulty.HARD, hard);
        return targets;
    }

    private Map<BloomLevel, Integer> bloomTargets(
            int remember,
            int understand,
            int apply,
            int analyze,
            int evaluate,
            int create) {
        Map<BloomLevel, Integer> targets = new EnumMap<>(BloomLevel.class);
        targets.put(BloomLevel.REMEMBER, remember);
        targets.put(BloomLevel.UNDERSTAND, understand);
        targets.put(BloomLevel.APPLY, apply);
        targets.put(BloomLevel.ANALYZE, analyze);
        targets.put(BloomLevel.EVALUATE, evaluate);
        targets.put(BloomLevel.CREATE, create);
        return targets;
    }
}
