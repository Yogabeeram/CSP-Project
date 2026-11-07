package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.dto.TopicPdfDownload;
import com.college.questionpapergenerator.dto.TopicPdfSummary;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.entity.TopicPdf;
import com.college.questionpapergenerator.exception.InvalidTopicPdfException;
import com.college.questionpapergenerator.exception.ResourceNotFoundException;
import com.college.questionpapergenerator.repository.TopicPdfRepository;
import com.college.questionpapergenerator.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

@Service
public class TopicPdfService {

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final TopicPdfRepository topicPdfRepository;
    private final TopicRepository topicRepository;
    private final long maximumFileSize;

    public TopicPdfService(
            TopicPdfRepository topicPdfRepository,
            TopicRepository topicRepository,
            @Value("${app.topic-pdf.max-size-bytes:20971520}") long maximumFileSize) {
        this.topicPdfRepository = topicPdfRepository;
        this.topicRepository = topicRepository;
        this.maximumFileSize = maximumFileSize;
    }

    @Transactional(readOnly = true)
    public List<TopicPdfSummary> findAllSummaries() {
        return topicPdfRepository.findAllSummaries();
    }

    @Transactional
    public List<TopicPdfSummary> upload(Long topicId, List<MultipartFile> files) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic " + topicId + " was not found"));
        if (files == null || files.isEmpty()) {
            throw new InvalidTopicPdfException("Choose at least one PDF file");
        }

        List<TopicPdf> documents = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);
            byte[] content;
            try {
                content = file.getBytes();
            } catch (IOException exception) {
                throw new InvalidTopicPdfException("Unable to read the uploaded PDF", exception);
            }
            if (!hasPdfSignature(content)) {
                throw new InvalidTopicPdfException(
                        file.getOriginalFilename() + " is not a valid PDF document");
            }
            documents.add(new TopicPdf(topic, safeFilename(file.getOriginalFilename()), content));
        }

        return topicPdfRepository.saveAll(documents).stream()
                .map(saved -> new TopicPdfSummary(
                        saved.getId(),
                        topicId,
                        saved.getOriginalFilename(),
                        saved.getFileSize(),
                        saved.getUploadedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TopicPdfDownload download(Long topicId, Long pdfId) {
        TopicPdf pdf = topicPdfRepository.findByIdAndTopicId(pdfId, topicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PDF " + pdfId + " was not found for topic " + topicId));
        return new TopicPdfDownload(pdf.getOriginalFilename(), pdf.getFileData());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidTopicPdfException("Choose a non-empty PDF file");
        }
        if (file.getSize() > maximumFileSize) {
            throw new InvalidTopicPdfException(
                    "PDF files must not exceed " + maximumFileSize / (1024 * 1024) + " MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidTopicPdfException("Only .pdf files are supported");
        }
    }

    private boolean hasPdfSignature(byte[] content) {
        if (content.length < PDF_SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < PDF_SIGNATURE.length; index++) {
            if (content[index] != PDF_SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    private String safeFilename(String originalFilename) {
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (filename.isEmpty() || filename.length() > 255) {
            throw new InvalidTopicPdfException("The PDF filename must be between 1 and 255 characters");
        }
        return filename;
    }
}
