package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.dto.QuestionRequest;
import com.college.questionpapergenerator.dto.QuestionResponse;
import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.exception.InvalidQuestionDataException;
import com.college.questionpapergenerator.exception.ResourceNotFoundException;
import com.college.questionpapergenerator.repository.QuestionRepository;
import com.college.questionpapergenerator.repository.SubjectRepository;
import com.college.questionpapergenerator.repository.TopicRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    public QuestionService(
            QuestionRepository questionRepository,
            SubjectRepository subjectRepository,
            TopicRepository topicRepository) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
    }

    public QuestionResponse create(QuestionRequest request) {
        Subject subject = findSubject(request.subjectId());
        Topic topic = findTopic(request.topicId());
        validateTopicSubject(topic, subject);

        Question question = new Question(
                request.questionText().trim(),
                subject,
                topic,
                request.marks(),
                request.difficulty(),
                request.bloomLevel(),
                false
        );
        return toResponse(questionRepository.save(question));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> findAll() {
        return questionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse findById(Long id) {
        return toResponse(findQuestion(id));
    }

    public QuestionResponse update(Long id, QuestionRequest request) {
        Question question = findQuestion(id);
        Subject subject = findSubject(request.subjectId());
        Topic topic = findTopic(request.topicId());
        validateTopicSubject(topic, subject);

        String questionText = request.questionText().trim();
        boolean classificationChanged = !Objects.equals(question.getQuestionText(), questionText)
                || !Objects.equals(question.getSubject().getId(), subject.getId())
                || !Objects.equals(question.getTopic().getId(), topic.getId())
                || question.getDifficulty() != request.difficulty()
                || question.getBloomLevel() != request.bloomLevel();
        question.setQuestionText(questionText);
        question.setSubject(subject);
        question.setTopic(topic);
        question.setMarks(request.marks());
        question.setDifficulty(request.difficulty());
        question.setBloomLevel(request.bloomLevel());
        if (classificationChanged) {
            question.setApproved(false);
        }
        return toResponse(questionRepository.save(question));
    }

    public void delete(Long id) {
        Question question = findQuestion(id);
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> search(
            Long subjectId,
            Long topicId,
            Integer unitNumber,
            Difficulty difficulty,
            BloomLevel bloomLevel) {
        Specification<Question> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (subjectId != null) {
                predicates.add(criteriaBuilder.equal(root.get("subject").get("id"), subjectId));
            }
            if (topicId != null) {
                predicates.add(criteriaBuilder.equal(root.get("topic").get("id"), topicId));
            }
            if (unitNumber != null) {
                predicates.add(criteriaBuilder.equal(root.get("topic").get("unitNumber"), unitNumber));
            }
            if (difficulty != null) {
                predicates.add(criteriaBuilder.equal(root.get("difficulty"), difficulty));
            }
            if (bloomLevel != null) {
                predicates.add(criteriaBuilder.equal(root.get("bloomLevel"), bloomLevel));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return questionRepository.findAll(specification).stream()
                .map(this::toResponse)
                .toList();
    }

    private Question findQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question " + id + " was not found"));
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject " + id + " was not found"));
    }

    private Topic findTopic(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic " + id + " was not found"));
    }

    private void validateTopicSubject(Topic topic, Subject subject) {
        if (!topic.getSubject().getId().equals(subject.getId())) {
            throw new InvalidQuestionDataException("Topic does not belong to the specified subject");
        }
    }

    private QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getSubject().getId(),
                question.getSubject().getSubjectName(),
                question.getTopic().getId(),
                question.getTopic().getTopicName(),
                question.getTopic().getUnitNumber(),
                question.getMarks(),
                question.getDifficulty(),
                question.getBloomLevel(),
                question.getCreatedAt(),
                question.isApproved()
        );
    }
}
