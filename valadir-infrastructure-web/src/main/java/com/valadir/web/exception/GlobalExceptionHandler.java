package com.valadir.web.exception;

import com.valadir.application.exception.ApplicationException;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.AccountLockedException;
import com.valadir.domain.exception.DomainException;
import com.valadir.web.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final HttpStatusResolver httpStatusResolver;

    GlobalExceptionHandler(HttpStatusResolver httpStatusResolver) {

        this.httpStatusResolver = httpStatusResolver;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        @NonNull MethodArgumentNotValidException e,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {

        List<ErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> new ErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ErrorCode.INVALID_FIELD.getCode(), errors));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        @NonNull Exception e,
        Object body,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {

        logAtLevel(status, "Spring MVC error: " + e.getMessage(), e);

        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomain(DomainException e) {

        log.warn("Domain rule violation: {}", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getErrorCode().getCode()));
    }

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ErrorResponse> handleApplication(ApplicationException e) {

        HttpStatus status = httpStatusResolver.resolve(e.getErrorCode());
        logAtLevel(status, "Application error: " + e.getMessage(), e);

        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(e.getErrorCode().getCode()));
    }

    @ExceptionHandler(AccountLockedException.class)
    ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException e) {

        log.warn("Account temporarily locked: {}", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(e.lockout().toSeconds()))
            .body(new ErrorResponse(e.getErrorCode().getCode()));
    }

    @ExceptionHandler(InfrastructureException.class)
    ResponseEntity<ErrorResponse> handleInfrastructure(InfrastructureException e) {

        log.error("Infrastructure dependency unavailable: {}", e.getMessage(), e);

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(e.getErrorCode().getCode()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {

        log.error("Unexpected error", e);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    private static void logAtLevel(HttpStatusCode status, String message, Exception e) {

        if (status.is5xxServerError()) {
            log.error(message, e);
        } else {
            log.warn(message);
        }
    }
}
