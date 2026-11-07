package com.college.questionpapergenerator.exception;

public class InvalidExcelUploadException extends RuntimeException {

    public InvalidExcelUploadException(String message) {
        super(message);
    }

    public InvalidExcelUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
