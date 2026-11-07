package com.college.questionpapergenerator.service;

import com.college.questionpapergenerator.entity.Subject;
import com.college.questionpapergenerator.entity.Topic;
import com.college.questionpapergenerator.entity.TopicPdf;
import com.college.questionpapergenerator.exception.InvalidTopicPdfException;
import com.college.questionpapergenerator.repository.TopicPdfRepository;
import com.college.questionpapergenerator.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicPdfServiceTest {

    private final TopicPdfRepository topicPdfRepository = mock(TopicPdfRepository.class);
    private final TopicRepository topicRepository = mock(TopicRepository.class);
    private final TopicPdfService service = new TopicPdfService(topicPdfRepository, topicRepository, 20);
    private Topic topic;

    @BeforeEach
    void setUp() {
        topic = new Topic("Algorithms", 1, new Subject("Computer Science", "CS"));
        topic.setId(7L);
        when(topicRepository.findById(7L)).thenReturn(Optional.of(topic));
        when(topicPdfRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void uploadsMultiplePdfsForTheirTopicAndRemovesClientPathFromFilenames() {
        var first = pdf("C:\\uploads\\week-one.pdf", "%PDF-1.4 first");
        var second = pdf("week-two.pdf", "%PDF-1.7 second");

        var saved = service.upload(7L, List.of(first, second));

        assertEquals(2, saved.size());
        assertEquals(7L, saved.get(0).topicId());
        assertEquals("week-one.pdf", saved.get(0).originalFilename());
        assertEquals("week-two.pdf", saved.get(1).originalFilename());
        verify(topicPdfRepository).saveAll(anyList());
    }

    @Test
    void rejectsEmptyFilesOversizedFilesAndNonPdfContents() {
        assertThrows(InvalidTopicPdfException.class, () -> service.upload(7L, List.of(
                new MockMultipartFile("files", "empty.pdf", "application/pdf", new byte[0]))));
        assertThrows(InvalidTopicPdfException.class, () -> service.upload(7L, List.of(
                pdf("large.pdf", "%PDF-12345678901234567890"))));
        assertThrows(InvalidTopicPdfException.class, () -> service.upload(7L, List.of(
                new MockMultipartFile("files", "not-a-pdf.pdf", "application/pdf",
                        "not a PDF".getBytes(StandardCharsets.US_ASCII)))));

        verify(topicPdfRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsUnsupportedExtensions() {
        var file = new MockMultipartFile(
                "files", "notes.txt", "text/plain", "%PDF-1.4".getBytes(StandardCharsets.US_ASCII));

        assertThrows(InvalidTopicPdfException.class, () -> service.upload(7L, List.of(file)));

        verify(topicPdfRepository, never()).saveAll(anyList());
    }

    @Test
    void downloadsOnlyTheRequestedTopicPdf() {
        byte[] content = "%PDF-1.4 sample".getBytes(StandardCharsets.US_ASCII);
        TopicPdf topicPdf = new TopicPdf(topic, "reference.pdf", content);
        when(topicPdfRepository.findByIdAndTopicId(11L, 7L)).thenReturn(Optional.of(topicPdf));

        var download = service.download(7L, 11L);

        assertEquals("reference.pdf", download.originalFilename());
        assertArrayEquals(content, download.content());
        verify(topicPdfRepository).findByIdAndTopicId(11L, 7L);
    }

    private MockMultipartFile pdf(String filename, String content) {
        return new MockMultipartFile(
                "files", filename, "application/pdf", content.getBytes(StandardCharsets.US_ASCII));
    }
}
