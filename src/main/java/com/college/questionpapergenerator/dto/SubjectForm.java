package com.college.questionpapergenerator.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public class SubjectForm {

    @NotBlank
    @Size(max = 150)
    private String subjectName;

    @Size(max = 30)
    private String subjectCode;

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }
}
