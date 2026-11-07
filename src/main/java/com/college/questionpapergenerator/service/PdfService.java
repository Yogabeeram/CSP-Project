package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.entity.ExamPaper;
import com.college.questionpapergenerator.entity.ExamType;
import com.college.questionpapergenerator.entity.PaperQuestion;
import com.college.questionpapergenerator.exception.PdfGenerationException;
import com.college.questionpapergenerator.exception.ResourceNotFoundException;
import com.college.questionpapergenerator.repository.ExamPaperRepository;
import com.college.questionpapergenerator.repository.PaperQuestionRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

@Service
public class PdfService {

    private static final float MARGIN = 48;
    private static final float BOTTOM_MARGIN = 55;
    private static final float CONTENT_WIDTH = PDRectangle.A4.getWidth() - (MARGIN * 2);
    private static final float BODY_SIZE = 11;
    private static final float BODY_LINE_HEIGHT = 16;
    private static final DateTimeFormatter EXAM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
    private static final List<String> GENERAL_INSTRUCTIONS = List.of(
            "Answer all questions unless a section specifies otherwise.",
            "Write the question number clearly against each answer.",
            "Figures in brackets indicate the marks for each question.",
            "Do not use unauthorized materials or electronic devices."
    );

    private final ExamPaperRepository examPaperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final String collegeName;

    public PdfService(
            ExamPaperRepository examPaperRepository,
            PaperQuestionRepository paperQuestionRepository,
            @Value("${app.college.name:Your College Name}") String collegeName) {
        this.examPaperRepository = examPaperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.collegeName = collegeName;
    }

    @Transactional(readOnly = true)
    public byte[] generatePaperPdf(Long paperId) {
        ExamPaper paper = examPaperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam paper " + paperId + " was not found"));
        List<PaperQuestion> questions =
                paperQuestionRepository.findByExamPaperIdOrderByQuestionNumberAsc(paperId);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfLayout layout = new PdfLayout(document);
            renderPaper(layout, paper, questions);
            layout.close();
            addPageNumbers(document);
            document.save(output);
            return output.toByteArray();
        } catch (IOException | IllegalArgumentException exception) {
            throw new PdfGenerationException("Unable to generate the examination paper PDF", exception);
        }
    }

    private void renderPaper(PdfLayout layout, ExamPaper paper, List<PaperQuestion> questions) throws IOException {
        layout.drawCentered(collegeName, layout.boldFont, 17, 23);
        layout.drawCentered("QUESTION PAPER", layout.boldFont, 12, 21);
        layout.drawCentered(
                paper.getExamType() == ExamType.SEMESTER ? "SEMESTER EXAMINATION" : "MID EXAMINATION",
                layout.boldFont,
                12,
                21);
        layout.drawCentered(paper.getExamTitle(), layout.boldFont, 15, 25);
        layout.drawRule();

        layout.drawWrapped("Subject: " + paper.getSubject().getSubjectName(), layout.regularFont, 11, 16, 0);
        String examDate = paper.getExamDate() == null
                ? "To be announced"
                : EXAM_DATE_FORMAT.format(paper.getExamDate());
        layout.drawWrapped("Exam Date: " + examDate, layout.regularFont, 11, 16, 0);
        layout.drawWrapped("Duration: " + paper.getDuration() + " minutes", layout.regularFont, 11, 16, 0);
        layout.drawWrapped("Maximum Marks: " + paper.getTotalMarks(), layout.regularFont, 11, 16, 0);
        layout.addVerticalSpace(10);

        layout.ensureSpace(40);
        layout.drawWrapped("General Instructions", layout.boldFont, 12, 17, 0);
        for (int index = 0; index < GENERAL_INSTRUCTIONS.size(); index++) {
            layout.drawWrapped((index + 1) + ". " + GENERAL_INSTRUCTIONS.get(index),
                    layout.regularFont, BODY_SIZE, BODY_LINE_HEIGHT, 12);
        }
        layout.addVerticalSpace(8);

        String currentSection = null;
        for (PaperQuestion paperQuestion : questions) {
            if (!paperQuestion.getSectionName().equals(currentSection)) {
                if (currentSection != null) {
                    layout.addVerticalSpace(8);
                }
                currentSection = paperQuestion.getSectionName();
                layout.ensureSpace(32);
                String sectionHeading = currentSection.startsWith("Section ")
                        ? currentSection
                        : "Section " + currentSection;
                layout.drawWrapped(sectionHeading, layout.boldFont, 13, 19, 0);
            }
            layout.drawQuestion(paperQuestion);
        }
    }

    private void addPageNumbers(PDDocument document) throws IOException {
        int pageCount = document.getNumberOfPages();
        for (int index = 0; index < pageCount; index++) {
            PDPage page = document.getPage(index);
            String footer = "Page " + (index + 1) + " of " + pageCount;
            PDType1Font footerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float textWidth = footerFont.getStringWidth(footer) / 1000 * 9;
            try (PDPageContentStream stream = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true)) {
                stream.setFont(footerFont, 9);
                stream.beginText();
                stream.newLineAtOffset((PDRectangle.A4.getWidth() - textWidth) / 2, 28);
                stream.showText(footer);
                stream.endText();
            }
        }
    }

    private static final class PdfLayout {

        private final PDDocument document;
        private final PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private PdfLayout(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void drawCentered(String text, PDType1Font font, float fontSize, float lineHeight)
                throws IOException {
            ensureSpace(lineHeight);
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            float x = Math.max(MARGIN, (PDRectangle.A4.getWidth() - textWidth) / 2);
            drawLine(text, font, fontSize, x);
            y -= lineHeight;
        }

        private void drawWrapped(String text, PDType1Font font, float fontSize, float lineHeight, float indent)
                throws IOException {
            float x = MARGIN + indent;
            float width = CONTENT_WIDTH - indent;
            for (String line : wrap(text, font, fontSize, width)) {
                ensureSpace(lineHeight);
                drawLine(line, font, fontSize, x);
                y -= lineHeight;
            }
        }

        private void drawQuestion(PaperQuestion paperQuestion) throws IOException {
            ensureSpace(BODY_LINE_HEIGHT * 2);
            String label = paperQuestion.getQuestionNumber() + ".";
            drawLine(label, boldFont, BODY_SIZE, MARGIN);
            String marks = "[" + paperQuestion.getMarks() + " marks]";
            float marksWidth = regularFont.getStringWidth(marks) / 1000 * BODY_SIZE;
            drawLine(marks, regularFont, 9, MARGIN + CONTENT_WIDTH - marksWidth);
            y -= BODY_LINE_HEIGHT;

            drawWrapped(
                    paperQuestion.getQuestion().getQuestionText(),
                    regularFont,
                    BODY_SIZE,
                    BODY_LINE_HEIGHT,
                    18
            );
            y -= 8;
        }

        private void drawRule() throws IOException {
            ensureSpace(12);
            stream.setLineWidth(0.7f);
            stream.moveTo(MARGIN, y);
            stream.lineTo(MARGIN + CONTENT_WIDTH, y);
            stream.stroke();
            y -= 14;
        }

        private void drawLine(String text, PDType1Font font, float fontSize, float x) throws IOException {
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
        }

        private List<String> wrap(String text, PDType1Font font, float fontSize, float maxWidth)
                throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String paragraphLine : text.replace("\r", "").split("\n", -1)) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                for (String word : paragraphLine.split("\\s+")) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    if (line.isEmpty()) {
                        appendLongWord(lines, line, word, font, fontSize, maxWidth);
                    } else {
                        String candidate = line + " " + word;
                        if (font.getStringWidth(candidate) / 1000 * fontSize <= maxWidth) {
                            line.append(' ').append(word);
                        } else {
                            lines.add(line.toString());
                            line.setLength(0);
                            appendLongWord(lines, line, word, font, fontSize, maxWidth);
                        }
                    }
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private void appendLongWord(
                List<String> lines,
                StringBuilder line,
                String word,
                PDType1Font font,
                float fontSize,
                float maxWidth) throws IOException {
            StringBuilder part = new StringBuilder();
            for (int offset = 0; offset < word.length(); ) {
                int codePoint = word.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                String candidate = part + character;
                if (!part.isEmpty() && font.getStringWidth(candidate) / 1000 * fontSize > maxWidth) {
                    lines.add(part.toString());
                    part.setLength(0);
                }
                part.append(character);
                offset += Character.charCount(codePoint);
            }
            line.append(part);
        }

        private void addVerticalSpace(float amount) {
            y -= amount;
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < BOTTOM_MARGIN) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }
    }
}
