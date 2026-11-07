package com.college.questionpapergenerator.exception;

public class AIClassificationException extends RuntimeException {

    public AIClassificationException(String message) {
        super(message);
    }

    public AIClassificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
