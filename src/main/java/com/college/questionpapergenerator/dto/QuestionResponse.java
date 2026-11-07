package com.college.questionpapergenerator.dto;

import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;

import java.time.LocalDateTime;

public record QuestionResponse(
        Long id,
        String questionText,
        Long subjectId,
        String subjectName,
        Long topicId,
        String topicName,
        Integer unitNumber,
        Integer marks,
        Difficulty difficulty,
        BloomLevel bloomLevel,
        LocalDateTime createdAt,
        boolean approved
) {
}
