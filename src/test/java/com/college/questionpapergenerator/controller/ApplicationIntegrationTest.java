package com.college.questionpapergenerator.controller;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.ExamPaper;
import com.college.questionpapergenerator.entity.PaperQuestion;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.repository.ExamPaperRepository;
import com.college.questionpapergenerator.repository.QuestionRepository;
import com.college.questionpapergenerator.repository.SubjectRepository;
import com.college.questionpapergenerator.repository.TopicPdfRepository;
import com.college.questionpapergenerator.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:qpgtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test-key",
        "app.college.name=Integration Test College"
})
class ApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicPdfRepository topicPdfRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamPaperRepository examPaperRepository;

    private Subject subject;
    private Topic topic;
    private Question approvedQuestion;
    private ExamPaper paper;

    @BeforeEach
    void setUp() {
        topicPdfRepository.deleteAll();
        examPaperRepository.deleteAll();
        questionRepository.deleteAll();
        topicRepository.deleteAll();
        subjectRepository.deleteAll();

        subject = subjectRepository.save(new Subject("Computer Science", "CS"));
        topic = topicRepository.save(new Topic("Algorithms", 1, subject));
        approvedQuestion = questionRepository.save(new Question(
                "Explain binary search.", subject, topic, 5,
                Difficulty.MEDIUM, BloomLevel.UNDERSTAND, true));

        paper = new ExamPaper("Algorithms Test", subject, 5, 1, 60);
        paper.setPaperQuestions(List.of(new PaperQuestion(
                paper, approvedQuestion, 1, "A", 5)));
        paper = examPaperRepository.save(paper);

        questionRepository.save(new Question(
                "Classify this pending question.", subject, topic, 2,
                Difficulty.EASY, BloomLevel.REMEMBER, false));
    }

    @Test
    void rendersAllDatabaseBackedThymeleafPages() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Algorithms Test")));
        mockMvc.perform(get("/questions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Explain binary search.")));
        mockMvc.perform(get("/questions/new")).andExpect(status().isOk());
        mockMvc.perform(get("/questions/{id}/edit", approvedQuestion.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Explain binary search.")));
        mockMvc.perform(get("/upload")).andExpect(status().isOk());
        mockMvc.perform(get("/review"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Classify this pending question.")));
        mockMvc.perform(get("/subjects"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Computer Science")));
        mockMvc.perform(get("/topics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Algorithms")))
                .andExpect(content().string(containsString("Upload PDFs")));
        mockMvc.perform(get("/papers/generate"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Computer Science")))
                .andExpect(content().string(containsString("Semester Examination")));
        mockMvc.perform(get("/papers/{id}", paper.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Explain binary search.")));
        mockMvc.perform(get("/papers/{id}/download", paper.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Download PDF")));
    }

    @Test
    void createsSubjectWhenCodeIsOmitted() throws Exception {
        mockMvc.perform(post("/subjects")
                        .param("subjectName", "Software Engineering"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/subjects"));

        Subject created = subjectRepository.findBySubjectNameIgnoreCase("Software Engineering")
                .stream()
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(created.getSubjectCode().isBlank());
    }

    @Test
    void exposesQuestionCrudSearchAndPaperPdfEndpoints() throws Exception {
        mockMvc.perform(get("/api/questions/{id}", approvedQuestion.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"topicName\":\"Algorithms\"")));
        mockMvc.perform(get("/api/questions/search")
                        .param("subjectId", subject.getId().toString())
                        .param("unitNumber", "1")
                        .param("bloomLevel", "UNDERSTAND"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Explain binary search.")));
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionText":"Explain merge sort.",
                                  "subjectId":%d,
                                  "topicId":%d,
                                  "marks":5,
                                  "difficulty":"MEDIUM",
                                  "bloomLevel":"UNDERSTAND"
                                }
                                """.formatted(subject.getId(), topic.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(content().string(containsString("\"approved\":false")));
        mockMvc.perform(get("/api/papers/{id}/pdf", paper.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    void uploadsMultipleTopicPdfsAndDownloadsTheSavedFile() throws Exception {
        byte[] firstPdf = "%PDF-1.4 first topic handout".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] secondPdf = "%PDF-1.7 second topic handout".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        mockMvc.perform(multipart("/topics/{topicId}/pdfs", topic.getId())
                        .file(new MockMultipartFile("files", "week-one.pdf", "application/pdf", firstPdf))
                        .file(new MockMultipartFile("files", "week-two.pdf", "application/pdf", secondPdf)))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/topics"));

        mockMvc.perform(get("/topics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("week-one.pdf")))
                .andExpect(content().string(containsString("week-two.pdf")));

        var savedPdf = topicPdfRepository.findAllSummaries().stream()
                .filter(item -> item.originalFilename().equals("week-one.pdf"))
                .findFirst()
                .orElseThrow();
        mockMvc.perform(get("/topics/{topicId}/pdfs/{pdfId}", topic.getId(), savedPdf.id()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(firstPdf));
    }

    @Test
    void opensTopicPdfPageFromItsDirectUrl() throws Exception {
        mockMvc.perform(get("/topics/{topicId}/pdfs", topic.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/topics#topic-" + topic.getId()));
    }
}
