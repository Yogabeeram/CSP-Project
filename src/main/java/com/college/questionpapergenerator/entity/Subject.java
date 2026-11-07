package com.college.questionpapergenerator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "subject_name", nullable = false, length = 150)
    private String subjectName;

    @NotBlank
    @Size(max = 30)
    @Column(name = "subject_code", nullable = false, unique = true, length = 30)
    private String subjectCode;

    @OneToMany(mappedBy = "subject")
    private List<Topic> topics = new ArrayList<>();

    @OneToMany(mappedBy = "subject")
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "subject")
    private List<ExamPaper> examPapers = new ArrayList<>();

    protected Subject() {
    }

    public Subject(String subjectName, String subjectCode) {
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<Topic> getTopics() {
        return topics;
    }

    public void setTopics(List<Topic> topics) {
        this.topics = topics;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public List<ExamPaper> getExamPapers() {
        return examPapers;
    }

    public void setExamPapers(List<ExamPaper> examPapers) {
        this.examPapers = examPapers;
    }
}
