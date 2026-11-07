package com.college.questionpapergenerator.exception;

public class InvalidTopicPdfException extends RuntimeException {

    public InvalidTopicPdfException(String message) {
        super(message);
    }

    public InvalidTopicPdfException(String message, Throwable cause) {
        super(message, cause);
    }
}
