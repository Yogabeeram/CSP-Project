package com.college.questionpapergenerator.dto;

import java.time.LocalDateTime;

public record TopicPdfSummary(
        Long id,
        Long topicId,
        String originalFilename,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}
