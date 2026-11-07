package com.college.questionpapergenerator.dto;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.ExamType;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

public record GeneratedPaperResponse(
        Long id,
        String examTitle,
        ExamType examType,
        Long subjectId,
        Integer totalMarks,
        Integer totalQuestions,
        Integer duration,
        LocalDate examDate,
        LocalDateTime createdAt,
        BloomDistributionReport bloomDistribution,
        List<GeneratedPaperQuestion> questions
) {
    public GeneratedPaperResponse {
        questions = List.copyOf(questions);
    }

    public record GeneratedPaperQuestion(
            Integer questionNumber,
            String sectionName,
            Long questionId,
            String questionText,
            Integer marks,
            Difficulty difficulty,
            BloomLevel bloomLevel,
            Integer unitNumber
    ) {
    }
}
