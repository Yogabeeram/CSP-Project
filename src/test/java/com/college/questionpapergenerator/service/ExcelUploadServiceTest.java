package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.exception.InvalidExcelUploadException;
import com.college.questionpapergenerator.repository.QuestionRepository;
import com.college.questionpapergenerator.repository.SubjectRepository;
import com.college.questionpapergenerator.repository.TopicRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExcelUploadServiceTest {

    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final SubjectRepository subjectRepository = mock(SubjectRepository.class);
    private final TopicRepository topicRepository = mock(TopicRepository.class);
    private final ExcelUploadService service =
            new ExcelUploadService(questionRepository, subjectRepository, topicRepository);
    private Subject subject;
    private Topic topic;

    @BeforeEach
    void setUp() {
        subject = new Subject("Computer Science", "CS");
        subject.setId(1L);
        topic = new Topic("Algorithms", 1, subject);
        topic.setId(2L);
        when(subjectRepository.findBySubjectNameIgnoreCase("Computer Science"))
                .thenReturn(List.of(subject));
        when(topicRepository.findBySubjectIdAndUnitNumberAndTopicNameIgnoreCase(1L, 1, "Algorithms"))
                .thenReturn(Optional.of(topic));
        when(questionRepository.existsBySubjectIdAndTopicIdAndQuestionTextIgnoreCase(
                1L, 2L, "Explain sorting"))
                .thenReturn(false);
    }

    @Test
    void importsValidRowsAndReportsInvalidRowsWithExcelNumbers() throws Exception {
        MockMultipartFile file = workbook(
                List.of("Question", "Subject", "Topic", "Unit", "Marks", "Difficulty", "BloomLevel"),
                List.of(
                        List.of("Explain sorting", "Computer Science", "Algorithms", "1", "5", "medium", "apply"),
                        List.of("Invalid difficulty", "Computer Science", "Algorithms", "1", "5", "expert", "apply"),
                        List.of("", "", "", "", "", "", "")));

        var report = service.upload(file);

        assertEquals(2, report.totalRows());
        assertEquals(1, report.successfullyImported());
        assertEquals(1, report.failedRows());
        assertEquals(3, report.errors().get(0).rowNumber());
        verify(questionRepository).saveAll(org.mockito.ArgumentMatchers.argThat(savedQuestions -> {
            var imported = savedQuestions.iterator().next();
            assertEquals(Difficulty.MEDIUM, imported.getDifficulty());
            assertEquals(BloomLevel.APPLY, imported.getBloomLevel());
            assertFalse(imported.isApproved());
            return true;
        }));
    }

    @Test
    void skipsDuplicatesWithinTheSameWorkbook() throws Exception {
        MockMultipartFile file = workbook(
                List.of("Question", "Subject", "Topic", "Unit", "Marks", "Difficulty", "BloomLevel"),
                List.of(
                        List.of("Explain sorting", "Computer Science", "Algorithms", "1", "5", "EASY", "REMEMBER"),
                        List.of("Explain sorting", "Computer Science", "Algorithms", "1", "5", "EASY", "REMEMBER")));

        var report = service.upload(file);

        assertEquals(2, report.totalRows());
        assertEquals(1, report.successfullyImported());
        assertEquals(1, report.failedRows());
        assertEquals("Duplicate question for this subject and topic", report.errors().get(0).message());
        verify(questionRepository).saveAll(org.mockito.ArgumentMatchers.argThat(
                savedQuestions -> savedQuestions.spliterator().getExactSizeIfKnown() == 1));
    }

    @Test
    void rejectsNonXlsxFilesBeforeParsing() {
        var file = new MockMultipartFile("file", "questions.csv", "text/csv", "not excel".getBytes());

        assertThrows(InvalidExcelUploadException.class, () -> service.upload(file));
    }

    @Test
    void rejectsMalformedXlsxWorkbook() {
        var file = new MockMultipartFile(
                "file",
                "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not a workbook".getBytes());

        assertThrows(InvalidExcelUploadException.class, () -> service.upload(file));
    }

    @Test
    void skipsQuestionsAlreadyPresentInTheDatabase() throws Exception {
        when(questionRepository.existsBySubjectIdAndTopicIdAndQuestionTextIgnoreCase(
                1L, 2L, "Explain sorting"))
                .thenReturn(true);
        MockMultipartFile file = workbook(
                List.of("Question", "Subject", "Topic", "Unit", "Marks", "Difficulty", "BloomLevel"),
                List.of(List.of("Explain sorting", "Computer Science", "Algorithms", "1", "5", "EASY", "REMEMBER")));

        var report = service.upload(file);

        assertEquals(1, report.totalRows());
        assertEquals(0, report.successfullyImported());
        assertEquals(1, report.failedRows());
        assertEquals("Duplicate question for this subject and topic", report.errors().get(0).message());
    }

    @Test
    void generatedTemplateContainsTheRequiredHeaders() throws Exception {
        byte[] template = service.createTemplate();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(template))) {
            Row header = workbook.getSheetAt(0).getRow(0);
            assertEquals("Question", header.getCell(0).getStringCellValue());
            assertEquals("Subject", header.getCell(1).getStringCellValue());
            assertEquals("Topic", header.getCell(2).getStringCellValue());
            assertEquals("Unit", header.getCell(3).getStringCellValue());
            assertEquals("Marks", header.getCell(4).getStringCellValue());
            assertEquals("Difficulty", header.getCell(5).getStringCellValue());
            assertEquals("BloomLevel", header.getCell(6).getStringCellValue());
        }
    }

    private MockMultipartFile workbook(List<String> headers, List<List<String>> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Questions");
            writeRow(sheet.createRow(0), headers);
            for (int index = 0; index < rows.size(); index++) {
                writeRow(sheet.createRow(index + 1), rows.get(index));
            }
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "questions.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }

    private void writeRow(Row row, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }
}
