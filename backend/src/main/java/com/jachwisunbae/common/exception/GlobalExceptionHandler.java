package com.jachwisunbae.common.exception;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.response.ErrorResponse;
import com.jachwisunbae.common.response.FieldErrorResponse;
import com.jachwisunbae.common.response.SensitiveRejectedValuePolicy;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final SensitiveRejectedValuePolicy sensitiveRejectedValuePolicy;

    public GlobalExceptionHandler(final SensitiveRejectedValuePolicy sensitiveRejectedValuePolicy) {
        this.sensitiveRejectedValuePolicy = sensitiveRejectedValuePolicy;
    }

    @ExceptionHandler(JachwiException.class)
    public ResponseEntity<ErrorResponse> handleJachwiException(final JachwiException exception) {
        final ErrorCode errorCode = exception.getErrorCode();
        if (errorCode.getStatus().is5xxServerError()) {
            log.error(
                    "application error: code={}, exceptionType={}",
                    errorCode.name(),
                    exception.getClass().getSimpleName()
            );
        }
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.from(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException exception
    ) {
        final List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorResponse)
                .toList();
        return ResponseEntity.badRequest().body(ErrorResponse.withErrors(ErrorCode.INVALID_REQUEST, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(final ConstraintViolationException exception) {
        final List<FieldErrorResponse> errors = exception.getConstraintViolations().stream()
                .map(violation -> {
                    final String field = violation.getPropertyPath().toString();
                    return new FieldErrorResponse(
                            field,
                            sensitiveRejectedValuePolicy.sanitize(field, violation.getInvalidValue()),
                            violation.getMessage()
                    );
                })
                .toList();
        return ResponseEntity.badRequest().body(ErrorResponse.withErrors(ErrorCode.INVALID_REQUEST, errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable() {
        return ResponseEntity.badRequest().body(ErrorResponse.from(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch() {
        return ResponseEntity.badRequest().body(ErrorResponse.from(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded() {
        return ResponseEntity.badRequest().body(ErrorResponse.from(ErrorCode.PHOTO_SIZE_EXCEEDED));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPart() {
        return ResponseEntity.badRequest().body(ErrorResponse.from(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(final Exception exception) {
        log.error("unexpected error: exceptionType={}", exception.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(ErrorResponse.from(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private FieldErrorResponse toFieldErrorResponse(final FieldError fieldError) {
        return new FieldErrorResponse(
                fieldError.getField(),
                sensitiveRejectedValuePolicy.sanitize(fieldError.getField(), fieldError.getRejectedValue()),
                fieldError.getDefaultMessage()
        );
    }
}
