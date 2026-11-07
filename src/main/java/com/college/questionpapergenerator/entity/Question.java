package com.college.questionpapergenerator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer marks;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_level", nullable = false, length = 10)
    private BloomLevel bloomLevel;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean approved;

    protected Question() {
    }

    public Question(String questionText, Subject subject, Topic topic, Integer marks,
                    Difficulty difficulty, BloomLevel bloomLevel, boolean approved) {
        this.questionText = questionText;
        this.subject = subject;
        this.topic = topic;
        this.marks = marks;
        this.difficulty = difficulty;
        this.bloomLevel = bloomLevel;
        this.approved = approved;
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

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Integer getMarks() {
        return marks;
    }

    public void setMarks(Integer marks) {
        this.marks = marks;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public BloomLevel getBloomLevel() {
        return bloomLevel;
    }

    public void setBloomLevel(BloomLevel bloomLevel) {
        this.bloomLevel = bloomLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public void updateClassification(Difficulty difficulty, BloomLevel bloomLevel, boolean approved) {
        this.difficulty = difficulty;
        this.bloomLevel = bloomLevel;
        this.approved = approved;
    }
}
