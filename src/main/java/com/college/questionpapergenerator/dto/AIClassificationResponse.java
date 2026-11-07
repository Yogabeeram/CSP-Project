package com.college.questionpapergenerator.dto;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;

public record AIClassificationResponse(
        Long questionId,
        Difficulty difficulty,
        BloomLevel bloomLevel,
        boolean approved
) {
}
