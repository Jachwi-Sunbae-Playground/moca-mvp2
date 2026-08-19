package com.jachwisunbae.common.web.error;

import com.jachwisunbae.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class BusinessExceptionHandler {

    private static final String INVALID_REQUEST_MESSAGE = "요청 값이 올바르지 않습니다.";

    private final DomainErrorHttpMapper httpMapper;

    public BusinessExceptionHandler(DomainErrorHttpMapper httpMapper) {
        this.httpMapper = httpMapper;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<DomainErrorResponse> handleBusinessException(BusinessException exception) {
        HttpStatus status = httpMapper.statusOf(exception.getCode());

        logException(exception, status);

        DomainErrorResponse response = new DomainErrorResponse(
                exception.getCode().name(),
                INVALID_REQUEST_MESSAGE);

        return ResponseEntity.status(status).body(response);
    }

    private void logException(BusinessException exception, HttpStatus status) {
        String message = "BusinessException: code={}, status={}, debugMessage={}";

        if (status.is5xxServerError()) {
            log.error(
                    message,
                    exception.getCode(),
                    status.value(),
                    exception.getMessage(),
                    exception);
            return;
        }

        if (status == HttpStatus.UNAUTHORIZED
                || status == HttpStatus.FORBIDDEN
                || status == HttpStatus.CONFLICT) {
            log.warn(message, exception.getCode(), status.value(), exception.getMessage());
            return;
        }

        log.info(message, exception.getCode(), status.value(), exception.getMessage());
    }
}
