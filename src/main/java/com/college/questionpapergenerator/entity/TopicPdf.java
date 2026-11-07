package com.college.questionpapergenerator.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic_pdfs")
public class TopicPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @NotNull
    @Positive
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @NotNull
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @NotNull
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    protected TopicPdf() {
    }

    public TopicPdf(Topic topic, String originalFilename, byte[] fileData) {
        this.topic = topic;
        this.originalFilename = originalFilename;
        this.fileData = fileData.clone();
        this.fileSize = (long) fileData.length;
    }

    @PrePersist
    protected void setUploadTimestamp() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Topic getTopic() {
        return topic;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public byte[] getFileData() {
        return fileData.clone();
    }
}
