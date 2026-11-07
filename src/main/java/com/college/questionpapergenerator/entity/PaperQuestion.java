package com.college.questionpapergenerator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "paper_questions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_paper_question_number", columnNames = {"exam_paper_id", "question_number"}),
        @UniqueConstraint(name = "uk_paper_question", columnNames = {"exam_paper_id", "question_id"})
})
public class PaperQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "exam_paper_id", nullable = false)
    private ExamPaper examPaper;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @NotNull
    @Positive
    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @NotBlank
    @Size(max = 100)
    @Column(name = "section_name", nullable = false, length = 100)
    private String sectionName;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer marks;

    protected PaperQuestion() {
    }

    public PaperQuestion(ExamPaper examPaper, Question question, Integer questionNumber,
                         String sectionName, Integer marks) {
        this.examPaper = examPaper;
        this.question = question;
        this.questionNumber = questionNumber;
        this.sectionName = sectionName;
        this.marks = marks;
    }

    @PrePersist
    @PreUpdate
    protected void ensureQuestionIsApproved() {
        if (question == null || !question.isApproved()) {
            throw new IllegalStateException("Only faculty-approved questions can be added to an exam paper");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamPaper getExamPaper() {
        return examPaper;
    }

    public void setExamPaper(ExamPaper examPaper) {
        this.examPaper = examPaper;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public Integer getMarks() {
        return marks;
    }

    public void setMarks(Integer marks) {
        this.marks = marks;
    }
}
