package com.college.questionpapergenerator.repository;

import com.college.questionpapergenerator.dto.TopicPdfSummary;
import com.college.questionpapergenerator.entity.TopicPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicPdfRepository extends JpaRepository<TopicPdf, Long> {

    @Query("""
            select new com.college.questionpapergenerator.dto.TopicPdfSummary(
                pdf.id, pdf.topic.id, pdf.originalFilename, pdf.fileSize, pdf.uploadedAt)
            from TopicPdf pdf
            order by pdf.uploadedAt desc, pdf.id desc
            """)
    List<TopicPdfSummary> findAllSummaries();

    Optional<TopicPdf> findByIdAndTopicId(Long id, Long topicId);

    @Query("select count(pdf) from TopicPdf pdf where pdf.topic.id = :topicId")
    long countByTopicId(@Param("topicId") Long topicId);
}
