package com.college.questionpapergenerator.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidQuestionDataException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidQuestionData(
            InvalidQuestionDataException exception,
            HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidExcelUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidExcelUpload(
            InvalidExcelUploadException exception,
            HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AIClassificationException.class)
    public ResponseEntity<ApiErrorResponse> handleAIClassificationFailure(
            AIClassificationException exception,
            HttpServletRequest request) {
        return buildError(HttpStatus.BAD_GATEWAY, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PaperGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handlePaperGenerationFailure(
            PaperGenerationException exception,
            HttpServletRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PdfGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handlePdfGenerationFailure(
            PdfGenerationException exception,
            HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT,
                    "The request conflicts with existing database records",
                request,
                Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "Request validation failed", request, validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            validationErrors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "Request validation failed", request, validationErrors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            Exception exception,
            HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST,
                "Request contains an invalid value or malformed JSON", request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
