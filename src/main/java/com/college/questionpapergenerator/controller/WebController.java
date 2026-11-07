package com.college.questionpapergenerator.controller;

import com.college.questionpapergenerator.dto.SubjectForm;
import com.college.questionpapergenerator.dto.TopicForm;
import com.college.questionpapergenerator.dto.TopicPdfDownload;
import com.college.questionpapergenerator.dto.TopicPdfSummary;
import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.exception.InvalidTopicPdfException;
import com.college.questionpapergenerator.exception.ResourceNotFoundException;
import com.college.questionpapergenerator.repository.ExamPaperRepository;
import com.college.questionpapergenerator.repository.PaperQuestionRepository;
import com.college.questionpapergenerator.repository.QuestionRepository;
import com.college.questionpapergenerator.repository.SubjectRepository;
import com.college.questionpapergenerator.repository.TopicRepository;
import com.college.questionpapergenerator.service.QuestionService;
import com.college.questionpapergenerator.service.TopicPdfService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    @Value("${app.college.name}")
    private String collegeName;

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ExamPaperRepository examPaperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionService questionService;
    private final TopicPdfService topicPdfService;

    public WebController(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            ExamPaperRepository examPaperRepository,
            PaperQuestionRepository paperQuestionRepository,
            QuestionService questionService,
            TopicPdfService topicPdfService) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.examPaperRepository = examPaperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionService = questionService;
        this.topicPdfService = topicPdfService;
    }

    @ModelAttribute
    public void addNavigationData(Model model) {
        model.addAttribute("pageTitle", "Question Paper Studio");
        model.addAttribute("collegeName", collegeName);
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("questionCount", questionRepository.count());
        model.addAttribute("subjectCount", subjectRepository.count());
        model.addAttribute("paperCount", examPaperRepository.count());
        model.addAttribute("easyCount", questionRepository.countByDifficulty(Difficulty.EASY));
        model.addAttribute("mediumCount", questionRepository.countByDifficulty(Difficulty.MEDIUM));
        model.addAttribute("hardCount", questionRepository.countByDifficulty(Difficulty.HARD));
        model.addAttribute("recentPapers", examPaperRepository.findTop5ByOrderByCreatedAtDesc());
        model.addAttribute("pendingReviewCount", questionRepository.countByApprovedFalse());
        return "dashboard";
    }

    @GetMapping("/questions")
    public String questions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) Integer unitNumber,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) BloomLevel bloomLevel,
            Model model) {
        model.addAttribute("questions",
                questionService.search(subjectId, topicId, unitNumber, difficulty, bloomLevel));
        model.addAttribute("subjects", subjectRepository.findAll(Sort.by("subjectName")));
        model.addAttribute("topics", topicRepository.findAll(Sort.by("topicName")));
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("bloomLevels", BloomLevel.values());
        model.addAttribute("selectedSubjectId", subjectId);
        model.addAttribute("selectedTopicId", topicId);
        model.addAttribute("selectedUnitNumber", unitNumber);
        model.addAttribute("selectedDifficulty", difficulty);
        model.addAttribute("selectedBloomLevel", bloomLevel);
        return "questions";
    }

    @GetMapping("/questions/new")
    public String addQuestion(Model model) {
        model.addAttribute("question", null);
        addQuestionFormData(model);
        return "question-form";
    }

    @GetMapping("/questions/{id}/edit")
    public String editQuestion(@PathVariable Long id, Model model) {
        model.addAttribute("question", questionService.findById(id));
        addQuestionFormData(model);
        return "question-form";
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @GetMapping("/review")
    public String classificationReview(Model model) {
        model.addAttribute("questions", questionRepository.findByApprovedFalseOrderByIdAsc());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("bloomLevels", BloomLevel.values());
        return "review";
    }

    @GetMapping("/subjects")
    public String subjects(Model model) {
        model.addAttribute("subjects", subjectRepository.findAll(Sort.by("subjectName")));
        model.addAttribute("subjectForm", new SubjectForm());
        return "subjects";
    }

    @PostMapping("/subjects")
    public String createSubject(
            @Valid @ModelAttribute("subjectForm") SubjectForm subjectForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("subjects", subjectRepository.findAll(Sort.by("subjectName")));
            return "subjects";
        }
        String subjectCode = subjectForm.getSubjectCode() == null || subjectForm.getSubjectCode().isBlank()
                ? generateSubjectCode(subjectForm.getSubjectName())
                : subjectForm.getSubjectCode().trim().toUpperCase();
        if (subjectRepository.findBySubjectCode(subjectCode).isPresent()
                && subjectForm.getSubjectCode() != null
                && !subjectForm.getSubjectCode().isBlank()) {
            bindingResult.rejectValue("subjectCode", "duplicate", "That subject code is already registered");
            model.addAttribute("subjects", subjectRepository.findAll(Sort.by("subjectName")));
            return "subjects";
        }
        subjectRepository.save(new Subject(
                subjectForm.getSubjectName().trim(),
                subjectCode));
        redirectAttributes.addFlashAttribute("successMessage", "Subject added successfully.");
        return "redirect:/subjects";
    }

    private String generateSubjectCode(String subjectName) {
        String normalized = subjectName.toUpperCase().replaceAll("[^A-Z0-9]", "");
        String base = normalized.isBlank() ? "SUBJECT" : normalized.substring(0, Math.min(normalized.length(), 23));
        String candidate = base;
        int suffix = 1;
        while (subjectRepository.findBySubjectCode(candidate).isPresent()) {
            String suffixText = "-" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 30 - suffixText.length())) + suffixText;
        }
        return candidate;
    }

    @GetMapping("/topics")
    public String topics(Model model) {
        List<Topic> topics = topicRepository.findAll(Sort.by("subject.subjectName", "unitNumber", "topicName"));
        model.addAttribute("topics", topics);
        model.addAttribute("topicPdfs", topicPdfsByTopic(topics));
        model.addAttribute("subjects", subjectRepository.findAll(Sort.by("subjectName")));
        model.addAttribute("topicForm", new TopicForm());
        return "topics";
    }

    @GetMapping("/topics/{topicId}/pdfs")
    public String viewTopicPdfs(@PathVariable Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new ResourceNotFoundException("Topic " + topicId + " was not found");
        }
        return "redirect:/topics#topic-" + topicId;
    }

    @PostMapping(path = "/topics/{topicId}/pdfs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadTopicPdf(
            @PathVariable Long topicId,
            @RequestParam("files") List<MultipartFile> files,
            RedirectAttributes redirectAttributes) {
        try {
            List<TopicPdfSummary> uploaded = topicPdfService.upload(topicId, files);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Uploaded " + uploaded.size()
                            + (uploaded.size() == 1 ? " PDF" : " PDFs") + " successfully.");
        } catch (InvalidTopicPdfException | ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/topics";
    }

    @GetMapping("/topics/{topicId}/pdfs/{pdfId}")
    public ResponseEntity<byte[]> downloadTopicPdf(
            @PathVariable Long topicId,
            @PathVariable Long pdfId) {
        TopicPdfDownload pdf = topicPdfService.download(topicId, pdfId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.content().length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(pdf.originalFilename(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(pdf.content());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleOversizedTopicPdf(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Each PDF must not exceed 20 MB.");
        return "redirect:/topics";
    }

    @PostMapping("/topics")
    public String createTopic(
            @Valid @ModelAttribute("topicForm") TopicForm topicForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            populateTopics(model);
            return "topics";
        }
        Subject subject = subjectRepository.findById(topicForm.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject " + topicForm.getSubjectId() + " was not found"));
        if (topicRepository.existsBySubjectIdAndUnitNumberAndTopicNameIgnoreCase(
                subject.getId(), topicForm.getUnitNumber(), topicForm.getTopicName().trim())) {
            bindingResult.rejectValue("topicName", "duplicate",
                    "That topic already exists in this subject and unit");
            populateTopics(model);
            return "topics";
        }
        topicRepository.save(new Topic(
                topicForm.getTopicName().trim(),
                topicForm.getUnitNumber(),
                subject));
        redirectAttributes.addFlashAttribute("successMessage", "Topic added successfully.");
        return "redirect:/topics";
    }

    @GetMapping("/papers/generate")
    public String generatePaperPage(Model model) {
        model.addAttribute("subjects", subjectRepository.findAll(Sort.by("subjectName")));
        model.addAttribute("topics", topicRepository.findAll(Sort.by("unitNumber", "topicName")));
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("bloomLevels", BloomLevel.values());
        return "generate";
    }

    @GetMapping("/papers/{id}")
    public String paperPreview(@PathVariable Long id, Model model) {
        var paper = examPaperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam paper " + id + " was not found"));
        var paperQuestions = paperQuestionRepository.findByExamPaperIdOrderByQuestionNumberAsc(id);
        java.util.Map<BloomLevel, Long> bloomCounts = new java.util.EnumMap<>(BloomLevel.class);
        for (BloomLevel level : BloomLevel.values()) {
            bloomCounts.put(level, paperQuestions.stream()
                    .filter(item -> item.getQuestion().getBloomLevel() == level)
                    .count());
        }
        model.addAttribute("paper", paper);
        model.addAttribute("paperQuestions", paperQuestions);
        model.addAttribute("bloomCounts", bloomCounts);
        return "paper-preview";
    }

    @GetMapping("/papers/{id}/download")
    public String paperDownloadPage(@PathVariable Long id, Model model) {
        var paper = examPaperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam paper " + id + " was not found"));
        model.addAttribute("paper", paper);
        return "download";
    }

    private void addQuestionFormData(Model model) {
        model.addAttribute("subjects", subjectRepository.findAll(Sort.by("subjectName")));
        model.addAttribute("topics", topicRepository.findAll(Sort.by("unitNumber", "topicName")));
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("bloomLevels", BloomLevel.values());
    }

    private void populateTopics(Model model) {
        List<Topic> topics = topicRepository.findAll(Sort.by("subject.subjectName", "unitNumber", "topicName"));
        model.addAttribute("topics", topics);
        model.addAttribute("topicPdfs", topicPdfsByTopic(topics));
        model.addAttribute("subjects", subjectRepository.findAll(Sort.by("subjectName")));
    }

    private Map<Long, List<TopicPdfSummary>> topicPdfsByTopic(List<Topic> topics) {
        Map<Long, List<TopicPdfSummary>> result = new HashMap<>();
        topics.forEach(topic -> result.put(topic.getId(), new ArrayList<>()));
        for (TopicPdfSummary pdf : topicPdfService.findAllSummaries()) {
            result.computeIfAbsent(pdf.topicId(), ignored -> new ArrayList<>()).add(pdf);
        }
        return result;
    }
}
