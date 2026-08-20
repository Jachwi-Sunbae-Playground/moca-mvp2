package com.jachwisunbae.common.web.error;

import com.jachwisunbae.common.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_REQUEST = "INVALID_REQUEST";
    private static final String INVALID_REQUEST_MESSAGE = "요청 값이 올바르지 않습니다.";
    private static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";

    private final DomainErrorHttpMapper httpMapper;

    public GlobalExceptionHandler(final DomainErrorHttpMapper httpMapper) {
        this.httpMapper = httpMapper;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<DomainErrorResponse> handleBusinessException(final BusinessException exception) {
        HttpStatus status = httpMapper.statusOf(exception.getCode());
        logBusinessException(exception, status);
        return response(status, exception.getCode().name(), INVALID_REQUEST_MESSAGE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DomainErrorResponse> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorResponse)
                .toList();
        log.info("Request validation failed: errorCount={}", errors.size());
        return response(HttpStatus.BAD_REQUEST, INVALID_REQUEST, INVALID_REQUEST_MESSAGE, errors);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<DomainErrorResponse> handleBindException(final BindException exception) {
        List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorResponse)
                .toList();
        log.info("Request binding failed: errorCount={}", errors.size());
        return response(HttpStatus.BAD_REQUEST, INVALID_REQUEST, INVALID_REQUEST_MESSAGE, errors);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<DomainErrorResponse> handleConstraintViolation(final Exception exception) {
        log.info("Request constraint validation failed: type={}", exception.getClass().getSimpleName());
        return response(HttpStatus.BAD_REQUEST, INVALID_REQUEST, INVALID_REQUEST_MESSAGE);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<DomainErrorResponse> handleTypeMismatch(
            final MethodArgumentTypeMismatchException exception) {
        FieldErrorResponse error = new FieldErrorResponse(exception.getName(), "올바른 형식의 값이 아닙니다.");
        log.info("Request argument type mismatch: field={}", exception.getName());
        return response(HttpStatus.BAD_REQUEST, INVALID_REQUEST, INVALID_REQUEST_MESSAGE, List.of(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DomainErrorResponse> handleUnreadableMessage(
            final HttpMessageNotReadableException exception) {
        log.info("Request body is not readable: type={}", exception.getClass().getSimpleName());
        return response(HttpStatus.BAD_REQUEST, INVALID_REQUEST, INVALID_REQUEST_MESSAGE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<DomainErrorResponse> handleMissingParameter(
            final MissingServletRequestParameterException exception) {
        FieldErrorResponse error = new FieldErrorResponse(exception.getParameterName(), "필수 값입니다.");
        log.info("Required request parameter is missing: field={}", exception.getParameterName());
        return response(HttpStatus.BAD_REQUEST, INVALID_REQUEST, INVALID_REQUEST_MESSAGE, List.of(error));
    }

    @ExceptionHandler({MissingPathVariableException.class, MissingServletRequestPartException.class})
    public ResponseEntity<DomainErrorResponse> handleMissingRequestValue(final Exception exception) {
        log.info("Required request value is missing: type={}", exception.getClass().getSimpleName());
        return response(HttpStatus.BAD_REQUEST, INVALID_REQUEST, INVALID_REQUEST_MESSAGE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<DomainErrorResponse> handleMethodNotSupported(
            final HttpRequestMethodNotSupportedException exception) {
        log.info("Request method is not supported: method={}", exception.getMethod());
        return response(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 요청 메서드입니다.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<DomainErrorResponse> handleMediaTypeNotSupported(
            final HttpMediaTypeNotSupportedException exception) {
        log.info("Request media type is not supported");
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 미디어 타입입니다.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<DomainErrorResponse> handleMaxUploadSize(
            final MaxUploadSizeExceededException exception) {
        log.info("Upload size limit exceeded");
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "PHOTO_FILE_TOO_LARGE", "파일 크기 제한을 초과했습니다.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<DomainErrorResponse> handleNoResourceFound(final NoResourceFoundException exception) {
        log.info("Request resource was not found");
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<DomainErrorResponse> handleDataIntegrityViolation(
            final DataIntegrityViolationException exception) {
        log.warn("Data integrity violation: type={}", exception.getClass().getSimpleName());
        return response(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "요청이 현재 데이터 상태와 충돌합니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DomainErrorResponse> handleUnexpectedException(final Exception exception) {
        log.error("Unexpected server error: type={}", exception.getClass().getSimpleName(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE);
    }

    private ResponseEntity<DomainErrorResponse> response(
            final HttpStatus status,
            final String code,
            final String message) {
        return response(status, code, message, List.of());
    }

    private ResponseEntity<DomainErrorResponse> response(
            final HttpStatus status,
            final String code,
            final String message,
            final List<FieldErrorResponse> errors) {
        return ResponseEntity.status(status).body(new DomainErrorResponse(code, message, errors));
    }

    private String reasonOf(final String reason) {
        if (reason == null || reason.isBlank()) {
            return "올바르지 않은 값입니다.";
        }
        return reason;
    }

    private FieldErrorResponse toFieldErrorResponse(final FieldError error) {
        if (error.isBindingFailure()) {
            return new FieldErrorResponse(error.getField(), "올바른 형식의 값이 아닙니다.");
        }
        return new FieldErrorResponse(error.getField(), reasonOf(error.getDefaultMessage()));
    }

    private void logBusinessException(final BusinessException exception, final HttpStatus status) {
        String message = "BusinessException: code={}, status={}, debugMessage={}";
        if (status.is5xxServerError()) {
            log.error(message, exception.getCode(), status.value(), exception.getMessage(), exception);
            return;
        }
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN || status == HttpStatus.CONFLICT) {
            log.warn(message, exception.getCode(), status.value(), exception.getMessage());
            return;
        }
        log.info(message, exception.getCode(), status.value(), exception.getMessage());
    }
}
