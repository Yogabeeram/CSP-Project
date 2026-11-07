package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.dto.ExcelUploadReport;
import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.exception.InvalidExcelUploadException;
import com.college.questionpapergenerator.repository.QuestionRepository;
import com.college.questionpapergenerator.repository.SubjectRepository;
import com.college.questionpapergenerator.repository.TopicRepository;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ExcelUploadService {

    private static final List<String> REQUIRED_HEADERS = List.of(
            "question", "subject", "topic", "unit", "marks", "difficulty", "bloomlevel");

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    public ExcelUploadService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional
    public ExcelUploadReport upload(MultipartFile file) {
        validateFile(file);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new InvalidExcelUploadException("The workbook does not contain a worksheet");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new InvalidExcelUploadException("The worksheet must have the expected header row");
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, Integer> columns = readHeaders(headerRow, formatter);
            validateHeaders(columns);

            var formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<Question> validQuestions = new ArrayList<>();
            List<ExcelUploadReport.RowUploadError> errors = new ArrayList<>();
            Set<String> importedKeys = new HashSet<>();
            int totalRows = 0;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row, formatter, formulaEvaluator)) {
                    continue;
                }

                totalRows++;
                try {
                    Question question = parseQuestion(row, columns, formatter, formulaEvaluator, importedKeys);
                    validQuestions.add(question);
                } catch (IllegalArgumentException exception) {
                    errors.add(new ExcelUploadReport.RowUploadError(rowIndex + 1, exception.getMessage()));
                }
            }

            questionRepository.saveAll(validQuestions);
            return new ExcelUploadReport(
                    totalRows,
                    validQuestions.size(),
                    errors.size(),
                    errors
            );
        } catch (IOException exception) {
            throw new InvalidExcelUploadException("Unable to read the Excel workbook", exception);
        } catch (NotOfficeXmlFileException exception) {
            throw new InvalidExcelUploadException("The uploaded file is not a valid .xlsx workbook", exception);
        }
    }

    public byte[] createTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Questions");
            Row header = sheet.createRow(0);
            for (int index = 0; index < REQUIRED_HEADERS.size(); index++) {
                String title = switch (REQUIRED_HEADERS.get(index)) {
                    case "question" -> "Question";
                    case "subject" -> "Subject";
                    case "topic" -> "Topic";
                    case "unit" -> "Unit";
                    case "marks" -> "Marks";
                    case "difficulty" -> "Difficulty";
                    case "bloomlevel" -> "BloomLevel";
                    default -> throw new IllegalStateException("Unexpected template column");
                };
                header.createCell(index, CellType.STRING).setCellValue(title);
                sheet.setColumnWidth(index, Math.min(60, title.length() + 8) * 256);
            }
            sheet.setColumnWidth(0, 60 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new InvalidExcelUploadException("Unable to create the Excel template", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidExcelUploadException("Upload a non-empty .xlsx file");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new InvalidExcelUploadException("Only .xlsx files are supported");
        }
    }

    private Map<String, Integer> readHeaders(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            if (!header.isEmpty() && columns.putIfAbsent(header, cell.getColumnIndex()) != null) {
                throw new InvalidExcelUploadException("Duplicate column header: " + header);
            }
        }
        return columns;
    }

    private void validateHeaders(Map<String, Integer> columns) {
        List<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(header -> !columns.containsKey(header))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new InvalidExcelUploadException(
                    "Missing required column headers: " + String.join(", ", missingHeaders));
        }
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell, evaluator).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Question parseQuestion(
            Row row,
            Map<String, Integer> columns,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            Set<String> importedKeys) {
        String questionText = value(row, columns, "question", formatter, evaluator);
        String subjectName = value(row, columns, "subject", formatter, evaluator);
        String topicName = value(row, columns, "topic", formatter, evaluator);
        int unitNumber = positiveInteger(value(row, columns, "unit", formatter, evaluator), "Unit");
        int marks = positiveInteger(value(row, columns, "marks", formatter, evaluator), "Marks");
        Difficulty difficulty = enumValue(
                value(row, columns, "difficulty", formatter, evaluator), Difficulty.class, "Difficulty");
        BloomLevel bloomLevel = enumValue(
                value(row, columns, "bloomlevel", formatter, evaluator), BloomLevel.class, "BloomLevel");

        if (questionText.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty");
        }
        if (questionText.length() > 65_535) {
            throw new IllegalArgumentException("Question exceeds the maximum supported length");
        }
        if (subjectName.isBlank()) {
            throw new IllegalArgumentException("Subject must not be empty");
        }
        if (topicName.isBlank()) {
            throw new IllegalArgumentException("Topic must not be empty");
        }

        List<Subject> subjects = subjectRepository.findBySubjectNameIgnoreCase(subjectName);
        if (subjects.isEmpty()) {
            throw new IllegalArgumentException("Subject not found: " + subjectName);
        }
        if (subjects.size() > 1) {
            throw new IllegalArgumentException("Subject name is ambiguous: " + subjectName);
        }

        Subject subject = subjects.get(0);
        Topic topic = topicRepository.findBySubjectIdAndUnitNumberAndTopicNameIgnoreCase(
                        subject.getId(), unitNumber, topicName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Topic not found for subject and unit: " + topicName));

        String questionKey = duplicateKey(subject.getId(), topic.getId(), questionText);
        if (!importedKeys.add(questionKey)
                || questionRepository.existsBySubjectIdAndTopicIdAndQuestionTextIgnoreCase(
                        subject.getId(), topic.getId(), questionText)) {
            throw new IllegalArgumentException("Duplicate question for this subject and topic");
        }

        return new Question(questionText, subject, topic, marks, difficulty, bloomLevel, false);
    }

    private String value(
            Row row,
            Map<String, Integer> columns,
            String header,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        Cell cell = row.getCell(columns.get(header), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
    }

    private int positiveInteger(String value, String fieldName) {
        try {
            BigDecimal number = new BigDecimal(value);
            int integer = number.intValueExact();
            if (integer <= 0) {
                throw new ArithmeticException("not positive");
            }
            return integer;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(fieldName + " must be a positive whole number");
        }
    }

    private <E extends Enum<E>> E enumValue(String value, Class<E> enumType, String fieldName) {
        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    fieldName + " must be one of: " + String.join(", ", enumNames(enumType)));
        }
    }

    private <E extends Enum<E>> List<String> enumNames(Class<E> enumType) {
        return java.util.Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .toList();
    }

    private String duplicateKey(Long subjectId, Long topicId, String questionText) {
        return subjectId + ":" + topicId + ":" + questionText.trim().toLowerCase(Locale.ROOT);
    }
}
