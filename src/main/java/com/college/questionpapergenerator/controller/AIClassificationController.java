package com.college.questionpapergenerator.controller;

import com.college.questionpapergenerator.dto.AIClassificationBatchReport;
import com.college.questionpapergenerator.dto.AIClassificationResponse;
import com.college.questionpapergenerator.dto.ClassificationReviewRequest;
import com.college.questionpapergenerator.service.AIClassificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/questions")
public class AIClassificationController {

    private final AIClassificationService aiClassificationService;

    public AIClassificationController(AIClassificationService aiClassificationService) {
        this.aiClassificationService = aiClassificationService;
    }

    @PostMapping("/{id}/classify")
    public AIClassificationResponse classifyQuestion(@PathVariable @Positive Long id) {
        return aiClassificationService.classifyQuestion(id);
    }

    @PostMapping("/classify-all")
    public AIClassificationBatchReport classifyAll() {
        return aiClassificationService.classifyAllUnapprovedQuestions();
    }

    @PutMapping("/{id}/classification-review")
    public AIClassificationResponse reviewClassification(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ClassificationReviewRequest review) {
        return aiClassificationService.reviewClassification(id, review);
    }
}
