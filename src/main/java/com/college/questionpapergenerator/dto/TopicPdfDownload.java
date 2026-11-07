package com.college.questionpapergenerator.dto;

public record TopicPdfDownload(String originalFilename, byte[] content) {

    public TopicPdfDownload {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
