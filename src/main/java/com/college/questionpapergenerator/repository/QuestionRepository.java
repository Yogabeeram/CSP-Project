package com.college.questionpapergenerator.repository;

import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Difficulty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    boolean existsBySubjectIdAndTopicIdAndQuestionTextIgnoreCase(
            Long subjectId,
            Long topicId,
            String questionText);

    @EntityGraph(attributePaths = {"subject", "topic"})
    List<Question> findByApprovedFalseOrderByIdAsc();

    long countByDifficulty(Difficulty difficulty);

    long countByApprovedFalse();

    List<Question> findBySubject_IdAndApprovedTrueAndTopic_UnitNumberIn(
            Long subjectId,
            List<Integer> unitNumbers);
}
