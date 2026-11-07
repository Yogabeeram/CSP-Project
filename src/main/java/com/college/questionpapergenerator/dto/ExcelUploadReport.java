package com.college.questionpapergenerator.dto;

import java.util.List;

public record ExcelUploadReport(
        int totalRows,
        int successfullyImported,
        int failedRows,
        List<RowUploadError> errors
) {
    public ExcelUploadReport {
        errors = List.copyOf(errors);
    }

    public record RowUploadError(int rowNumber, String message) {
    }
}
