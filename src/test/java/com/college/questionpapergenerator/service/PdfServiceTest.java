package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.ExamPaper;
import com.college.questionpapergenerator.entity.ExamType;
import com.college.questionpapergenerator.entity.PaperQuestion;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.exception.ResourceNotFoundException;
import com.college.questionpapergenerator.repository.ExamPaperRepository;
import com.college.questionpapergenerator.repository.PaperQuestionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfServiceTest {

    @Test
    void rendersThePersistedQuestionsAndPaperDetailsAcrossMultiplePages() throws Exception {
        ExamPaperRepository examPaperRepository = mock(ExamPaperRepository.class);
        PaperQuestionRepository paperQuestionRepository = mock(PaperQuestionRepository.class);
        Subject subject = new Subject("Computer Science", "CS");
        subject.setId(3L);
        ExamPaper paper = new ExamPaper(
                "End Semester Examination", subject, 40, 2, 120, LocalDate.of(2026, 12, 15),
                ExamType.SEMESTER);
        paper.setId(18L);
        String longQuestion = IntStream.range(0, 1_200)
                .mapToObj(index -> "algorithm question text")
                .collect(Collectors.joining(" "));
        Question first = question(50L, subject, longQuestion, 20);
        Question second = question(51L, subject, "Explain the saved sorting algorithm.", 20);
        List<PaperQuestion> paperQuestions = List.of(
                new PaperQuestion(paper, first, 1, "A", 20),
                new PaperQuestion(paper, second, 2, "B", 20)
        );
        when(examPaperRepository.findById(18L)).thenReturn(Optional.of(paper));
        when(paperQuestionRepository.findByExamPaperIdOrderByQuestionNumberAsc(18L))
                .thenReturn(paperQuestions);

        PdfService service = new PdfService(examPaperRepository, paperQuestionRepository, "Example College");
        byte[] pdfBytes = service.generatePaperPdf(18L);

        try (var document = Loader.loadPDF(pdfBytes)) {
            assertTrue(document.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Example College"));
            assertTrue(text.contains("SEMESTER EXAMINATION"));
            assertTrue(text.contains("End Semester Examination"));
            assertTrue(text.contains("Computer Science"));
            assertTrue(text.contains("15 December 2026"));
            assertTrue(text.contains("120 minutes"));
            assertTrue(text.contains("Maximum Marks: 40"));
            assertTrue(text.contains("General Instructions"));
            assertTrue(text.contains("Section A"));
            assertTrue(text.contains("Section B"));
            assertFalse(text.contains("Section Section A"));
            assertFalse(text.contains("Section Section B"));
            assertTrue(text.contains("1."));
            assertTrue(text.contains("2."));
            assertTrue(text.contains("[20 marks]"));
            assertTrue(text.contains("Explain the saved sorting algorithm."));
            assertTrue(text.contains("Page 1 of"));
            assertTrue(text.contains("Page " + document.getNumberOfPages() + " of "
                    + document.getNumberOfPages()));
        }
    }

    @Test
    void reportsWhenTheSavedPaperDoesNotExist() {
        ExamPaperRepository examPaperRepository = mock(ExamPaperRepository.class);
        PaperQuestionRepository paperQuestionRepository = mock(PaperQuestionRepository.class);
        when(examPaperRepository.findById(999L)).thenReturn(Optional.empty());

        PdfService service = new PdfService(examPaperRepository, paperQuestionRepository, "Example College");

        assertThrows(ResourceNotFoundException.class, () -> service.generatePaperPdf(999L));
    }

    private Question question(Long id, Subject subject, String text, int marks) {
        Topic topic = new Topic("Algorithms", 1, subject);
        topic.setId(7L);
        Question question = new Question(
                text, subject, topic, marks, Difficulty.MEDIUM, BloomLevel.UNDERSTAND, true);
        question.setId(id);
        return question;
    }
}
