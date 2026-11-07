package com.college.questionpapergenerator.dto;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.ExamType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PaperGenerationRequest(
        @NotBlank
        @Size(max = 200)
        String examTitle,

        @NotNull
        @Positive
        Long subjectId,

        @NotEmpty
        List<@NotNull @Positive Integer> selectedUnits,

        @NotNull
        @Positive
        Integer totalMarks,

        @NotNull
        @Positive
        Integer totalQuestions,

        @NotNull
        @Positive
        Integer duration,

        @NotEmpty
        Map<@NotNull Difficulty, @NotNull BigDecimal> difficultyDistribution,

        @NotEmpty
        Map<@NotNull BloomLevel, @NotNull BigDecimal> bloomDistribution,

        Map<@NotNull @Positive Integer, @NotNull BigDecimal> unitWeightage,

        @Valid
        List<@Valid PaperSectionRequirement> sections,

        List<@NotNull @Positive Integer> questionMarks,

        LocalDate examDate,

        ExamType examType
) {
    public PaperGenerationRequest {
        if (examType == null) {
            examType = ExamType.MID;
        }
    }

    public PaperGenerationRequest(
            String examTitle,
            Long subjectId,
            List<Integer> selectedUnits,
            Integer totalMarks,
            Integer totalQuestions,
            Integer duration,
            Map<Difficulty, BigDecimal> difficultyDistribution,
            Map<BloomLevel, BigDecimal> bloomDistribution,
            Map<Integer, BigDecimal> unitWeightage,
            List<PaperSectionRequirement> sections,
            List<Integer> questionMarks) {
        this(examTitle, subjectId, selectedUnits, totalMarks, totalQuestions, duration,
                difficultyDistribution, bloomDistribution, unitWeightage, sections, questionMarks, null, ExamType.MID);
    }

    public PaperGenerationRequest(
            String examTitle,
            Long subjectId,
            List<Integer> selectedUnits,
            Integer totalMarks,
            Integer totalQuestions,
            Integer duration,
            Map<Difficulty, BigDecimal> difficultyDistribution,
            Map<BloomLevel, BigDecimal> bloomDistribution,
            Map<Integer, BigDecimal> unitWeightage,
            List<PaperSectionRequirement> sections,
            List<Integer> questionMarks,
            LocalDate examDate) {
        this(examTitle, subjectId, selectedUnits, totalMarks, totalQuestions, duration,
                difficultyDistribution, bloomDistribution, unitWeightage, sections, questionMarks, examDate, ExamType.MID);
    }
}
