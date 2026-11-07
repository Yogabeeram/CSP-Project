package com.college.questionpapergenerator.dto;

import java.util.List;

public record AIClassificationBatchReport(
        int totalQuestions,
        int classifiedQuestions,
        int failedQuestions,
        List<ClassificationFailure> failures
) {
    public AIClassificationBatchReport {
        failures = List.copyOf(failures);
    }

    public record ClassificationFailure(Long questionId, String message) {
    }
}
