package com.college.questionpapergenerator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PaperSectionRequirement(
        @NotBlank
        @Size(max = 100)
        String sectionName,

        @NotEmpty
        List<@Positive Integer> questionMarks
) {
}
