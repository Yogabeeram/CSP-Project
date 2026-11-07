package com.college.questionpapergenerator.repository;

import com.college.questionpapergenerator.entity.PaperQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {

    @EntityGraph(attributePaths = "question")
    List<PaperQuestion> findByExamPaperIdOrderByQuestionNumberAsc(Long examPaperId);
}
