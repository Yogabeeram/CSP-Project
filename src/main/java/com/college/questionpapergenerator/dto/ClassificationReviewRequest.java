package com.college.questionpapergenerator.dto;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import jakarta.validation.constraints.NotNull;

public record ClassificationReviewRequest(
        @NotNull Difficulty difficulty,
        @NotNull BloomLevel bloomLevel
) {
}
