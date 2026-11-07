package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.dto.GeneratedPaperResponse;
import com.college.questionpapergenerator.dto.PaperGenerationRequest;
import com.college.questionpapergenerator.dto.PaperSectionRequirement;
import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.ExamPaper;
import com.college.questionpapergenerator.entity.ExamType;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.repository.ExamPaperRepository;
import com.college.questionpapergenerator.repository.QuestionRepository;
import com.college.questionpapergenerator.repository.SubjectRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperGeneratorServiceTest {

    @Test
    void generatesAndPersistsPaperWithQuestions() {
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        ExamPaperRepository examPaperRepository = mock(ExamPaperRepository.class);
        Subject subject = new Subject("Computer Science", "CS");
        subject.setId(1L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(questionRepository.findBySubject_IdAndApprovedTrueAndTopic_UnitNumberIn(1L, List.of(1)))
                .thenReturn(List.of(
                        question(1, subject, Difficulty.EASY, BloomLevel.REMEMBER),
                        question(2, subject, Difficulty.EASY, BloomLevel.UNDERSTAND),
                        question(3, subject, Difficulty.MEDIUM, BloomLevel.APPLY),
                        question(4, subject, Difficulty.HARD, BloomLevel.ANALYZE)
                ));
        when(examPaperRepository.save(any(ExamPaper.class))).thenAnswer(invocation -> {
            ExamPaper paper = invocation.getArgument(0);
            paper.setId(99L);
            paper.setCreatedAt(LocalDateTime.of(2026, 10, 30, 10, 0));
            return paper;
        });

        PaperGeneratorService service = new PaperGeneratorService(
                questionRepository, subjectRepository, examPaperRepository, new BloomDistributionService());
        PaperGenerationRequest request = new PaperGenerationRequest(
                "Algorithms Midterm",
                1L,
                List.of(1),
                12,
                4,
                90,
                Map.of(
                        Difficulty.EASY, new BigDecimal("50"),
                        Difficulty.MEDIUM, new BigDecimal("25"),
                        Difficulty.HARD, new BigDecimal("25")),
                Map.of(
                        BloomLevel.REMEMBER, new BigDecimal("25"),
                        BloomLevel.UNDERSTAND, new BigDecimal("25"),
                        BloomLevel.APPLY, new BigDecimal("25"),
                        BloomLevel.ANALYZE, new BigDecimal("25"),
                        BloomLevel.EVALUATE, BigDecimal.ZERO,
                        BloomLevel.CREATE, BigDecimal.ZERO),
                Map.of(1, new BigDecimal("100")),
                List.of(new PaperSectionRequirement("Section A", List.of(3, 3, 3, 3))),
                null,
                null,
                ExamType.SEMESTER
        );

        GeneratedPaperResponse result = service.generate(request);

        assertEquals(99L, result.id());
        assertEquals(ExamType.SEMESTER, result.examType());
        assertEquals(12, result.totalMarks());
        assertEquals(4, result.questions().size());
        assertEquals(1, result.bloomDistribution().actualCounts().get(BloomLevel.REMEMBER));
        assertEquals(1, result.bloomDistribution().requiredCounts().get(BloomLevel.ANALYZE));
        assertEquals(12, result.questions().stream()
                .mapToInt(GeneratedPaperResponse.GeneratedPaperQuestion::marks)
                .sum());
        verify(examPaperRepository).save(any(ExamPaper.class));
    }

    private Question question(
            long id,
            Subject subject,
            Difficulty difficulty,
            BloomLevel bloomLevel) {
        Topic topic = new Topic("Algorithms", 1, subject);
        topic.setId(1L);
        Question question = new Question(
                "Question " + id, subject, topic, 3, difficulty, bloomLevel, true);
        question.setId(id);
        return question;
    }
}
