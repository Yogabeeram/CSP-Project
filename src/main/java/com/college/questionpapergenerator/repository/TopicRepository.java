package com.college.questionpapergenerator.repository;

import com.college.questionpapergenerator.entity.Topic;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    @Override
    @EntityGraph(attributePaths = "subject")
    List<Topic> findAll(Sort sort);

    List<Topic> findBySubjectIdOrderByUnitNumberAscTopicNameAsc(Long subjectId);

    Optional<Topic> findBySubjectIdAndUnitNumberAndTopicNameIgnoreCase(
            Long subjectId,
            Integer unitNumber,
            String topicName);

    boolean existsBySubjectIdAndUnitNumberAndTopicNameIgnoreCase(
            Long subjectId,
            Integer unitNumber,
            String topicName);
}
