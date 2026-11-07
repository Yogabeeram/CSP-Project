package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.dto.ClassificationReviewRequest;
import com.college.questionpapergenerator.entity.BloomLevel;
import com.college.questionpapergenerator.entity.Difficulty;
import com.college.questionpapergenerator.entity.Question;
import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.exception.AIClassificationException;
import com.college.questionpapergenerator.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIClassificationServiceTest {

    @Test
    void savesValidAiSuggestionWithoutApprovingQuestion() {
        var fixture = fixture("{\"difficulty\":\"medium\",\"bloomLevel\":\"apply\"}");

        var result = fixture.service.classifyQuestion(7L);

        assertEquals(Difficulty.MEDIUM, result.difficulty());
        assertEquals(BloomLevel.APPLY, result.bloomLevel());
        assertFalse(result.approved());
        verify(fixture.questionRepository).save(fixture.question);
    }

    @Test
    void rejectsInvalidAiClassificationWithoutSaving() {
        var fixture = fixture("{\"difficulty\":\"EXPERT\",\"bloomLevel\":\"APPLY\"}");

        assertThrows(AIClassificationException.class, () -> fixture.service.classifyQuestion(7L));
        verify(fixture.questionRepository, never()).save(fixture.question);
    }

    @Test
    void wrapsAiProviderErrorsForApiHandling() {
        var fixture = fixture(null);
        when(fixture.callResponse.content()).thenThrow(new IllegalStateException("provider unavailable"));

        var exception = assertThrows(
                AIClassificationException.class,
                () -> fixture.service.classifyQuestion(7L));

        assertEquals("AI provider request failed", exception.getMessage());
        verify(fixture.questionRepository, never()).save(fixture.question);
    }

    @Test
    void facultyReviewApprovesTheSelectedClassification() {
        var fixture = fixture("{\"difficulty\":\"EASY\",\"bloomLevel\":\"REMEMBER\"}");

        var result = fixture.service.reviewClassification(
                7L, new ClassificationReviewRequest(Difficulty.HARD, BloomLevel.EVALUATE));

        assertEquals(Difficulty.HARD, result.difficulty());
        assertEquals(BloomLevel.EVALUATE, result.bloomLevel());
        assertTrue(result.approved());
        verify(fixture.questionRepository).save(fixture.question);
    }

    private Fixture fixture(String response) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponse = mock(ChatClient.CallResponseSpec.class);
        QuestionRepository repository = mock(QuestionRepository.class);
        Subject subject = new Subject("Computer Science", "CS");
        subject.setId(1L);
        Topic topic = new Topic("Algorithms", 1, subject);
        topic.setId(2L);
        Question question = new Question(
                "Explain sorting", subject, topic, 5, Difficulty.EASY, BloomLevel.REMEMBER, false);
        question.setId(7L);

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(request);
        when(request.system(anyString())).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.call()).thenReturn(callResponse);
        when(callResponse.content()).thenReturn(response);
        when(repository.findById(7L)).thenReturn(Optional.of(question));
        when(repository.save(question)).thenReturn(question);

        return new Fixture(new AIClassificationService(builder, new ObjectMapper(), repository),
                repository, question, callResponse);
    }

    private record Fixture(
            AIClassificationService service,
            QuestionRepository questionRepository,
            Question question,
            ChatClient.CallResponseSpec callResponse) {
    }
}
