package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.dto.QuestionRequest;
import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.exception.InvalidQuestionDataException;
import com.college.questionpapergenerator.repository.QuestionRepository;
import com.college.questionpapergenerator.repository.SubjectRepository;
import com.college.questionpapergenerator.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionServiceTest {

    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final SubjectRepository subjectRepository = mock(SubjectRepository.class);
    private final TopicRepository topicRepository = mock(TopicRepository.class);
    private final QuestionService service =
            new QuestionService(questionRepository, subjectRepository, topicRepository);
    private Subject subject;
    private Topic topic;

    @BeforeEach
    void setUp() {
        subject = new Subject("Computer Science", "CS");
        subject.setId(1L);
        topic = new Topic("Algorithms", 1, subject);
        topic.setId(2L);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(topicRepository.findById(2L)).thenReturn(Optional.of(topic));
    }

    @Test
    void createsQuestionAsUnapprovedAndMapsDatabaseRelationships() {
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var result = service.create(request("Explain sorting"));

        assertEquals(10L, result.id());
        assertEquals("Explain sorting", result.questionText());
        assertEquals(1L, result.subjectId());
        assertEquals("Computer Science", result.subjectName());
        assertEquals(2L, result.topicId());
        assertEquals("Algorithms", result.topicName());
        assertFalse(result.approved());
    }

    @Test
    void resetsApprovalWhenQuestionTextChanges() {
        Question question = new Question(
                "Old text", subject, topic, 5, Difficulty.EASY, BloomLevel.REMEMBER, true);
        question.setId(10L);
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionRepository.save(question)).thenReturn(question);

        var result = service.update(10L, request("Updated text"));

        assertEquals("Updated text", result.questionText());
        assertFalse(result.approved());
    }

    @Test
    void rejectsTopicFromAnotherSubjectWithoutPersisting() {
        Subject otherSubject = new Subject("Mathematics", "MA");
        otherSubject.setId(3L);
        when(subjectRepository.findById(3L)).thenReturn(Optional.of(otherSubject));

        var request = new QuestionRequest(
                "Explain sorting", 3L, 2L, 5, Difficulty.EASY, BloomLevel.REMEMBER);

        assertThrows(InvalidQuestionDataException.class, () -> service.create(request));
        verify(questionRepository, never()).save(any(Question.class));
    }

    private QuestionRequest request(String questionText) {
        return new QuestionRequest(
                questionText, 1L, 2L, 5, Difficulty.EASY, BloomLevel.REMEMBER);
    }
}
