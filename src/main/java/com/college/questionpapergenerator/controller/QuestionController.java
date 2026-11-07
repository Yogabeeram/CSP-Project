package com.college.questionpapergenerator.controller;

import com.college.questionpapergenerator.dto.QuestionRequest;
import com.college.questionpapergenerator.dto.QuestionResponse;
import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.service.QuestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<QuestionResponse> create(@Valid @RequestBody QuestionRequest request) {
        QuestionResponse created = questionService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<QuestionResponse> findAll() {
        return questionService.findAll();
    }

    @GetMapping("/{id}")
    public QuestionResponse findById(@PathVariable @Positive Long id) {
        return questionService.findById(id);
    }

    @PutMapping("/{id}")
    public QuestionResponse update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody QuestionRequest request) {
        return questionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<QuestionResponse> search(
            @RequestParam(required = false) @Positive Long subjectId,
            @RequestParam(required = false) @Positive Long topicId,
            @RequestParam(required = false) @Positive Integer unitNumber,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) BloomLevel bloomLevel) {
        return questionService.search(subjectId, topicId, unitNumber, difficulty, bloomLevel);
    }
}
