package com.college.questionpapergenerator.dto;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record QuestionRequest(
        @NotBlank
        @Size(max = 65_535)
        String questionText,

        @NotNull
        @Positive
        Long subjectId,

        @NotNull
        @Positive
        Long topicId,

        @NotNull
        @Positive
        Integer marks,

        @NotNull
        Difficulty difficulty,

        @NotNull
        BloomLevel bloomLevel
) {
}
