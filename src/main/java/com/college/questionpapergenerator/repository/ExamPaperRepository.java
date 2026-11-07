package com.college.questionpapergenerator.repository;

import com.college.questionpapergenerator.entity.ExamPaper;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {

    @Override
    @EntityGraph(attributePaths = "subject")
    Optional<ExamPaper> findById(Long id);

    List<ExamPaper> findBySubjectIdOrderByCreatedAtDesc(Long subjectId);

    @EntityGraph(attributePaths = "subject")
    List<ExamPaper> findTop5ByOrderByCreatedAtDesc();
}
