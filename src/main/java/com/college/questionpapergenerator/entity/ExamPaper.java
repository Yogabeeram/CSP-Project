package com.college.questionpapergenerator.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_papers")
public class ExamPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "exam_title", nullable = false, length = 200)
    private String examTitle;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @NotNull
    @Positive
    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @NotNull
    @Positive
    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer duration;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", length = 16)
    private ExamType examType;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "examPaper", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaperQuestion> paperQuestions = new ArrayList<>();

    protected ExamPaper() {
    }

    public ExamPaper(String examTitle, Subject subject, Integer totalMarks,
                     Integer totalQuestions, Integer duration) {
        this(examTitle, subject, totalMarks, totalQuestions, duration, null);
    }

    public ExamPaper(String examTitle, Subject subject, Integer totalMarks,
                     Integer totalQuestions, Integer duration, LocalDate examDate) {
        this(examTitle, subject, totalMarks, totalQuestions, duration, examDate, ExamType.MID);
    }

    public ExamPaper(String examTitle, Subject subject, Integer totalMarks,
                     Integer totalQuestions, Integer duration, LocalDate examDate, ExamType examType) {
        this.examTitle = examTitle;
        this.subject = subject;
        this.totalMarks = totalMarks;
        this.totalQuestions = totalQuestions;
        this.duration = duration;
        this.examDate = examDate;
        this.examType = examType == null ? ExamType.MID : examType;
    }

    @PrePersist
    protected void setCreationTimestamp() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Integer totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public ExamType getExamType() {
        return examType == null ? ExamType.MID : examType;
    }

    public void setExamType(ExamType examType) {
        this.examType = examType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<PaperQuestion> getPaperQuestions() {
        return paperQuestions;
    }

    public void setPaperQuestions(List<PaperQuestion> paperQuestions) {
        this.paperQuestions = paperQuestions;
    }
}
