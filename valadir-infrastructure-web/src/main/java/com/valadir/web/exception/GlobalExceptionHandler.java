package com.valadir.web.exception;

import com.valadir.application.exception.ApplicationException;
import com.valadir.common.error.ErrorCode;
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

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        @NonNull final MethodArgumentNotValidException e,
        @NonNull final HttpHeaders headers,
        @NonNull final HttpStatusCode status,
        @NonNull final WebRequest request
    ) {

        final List<ErrorResponse.FieldError> errors = e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> new ErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ErrorCode.INVALID_FIELD.getCode(), errors));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        @NonNull final Exception e,
        final Object body,
        @NonNull final HttpHeaders headers,
        @NonNull final HttpStatusCode status,
        @NonNull final WebRequest request
    ) {

        logAtLevel(status, "Spring MVC error: " + e.getMessage(), e);

        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomain(final DomainException e) {

        log.warn("Domain rule violation: {}", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getErrorCode().getCode()));
    }

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ErrorResponse> handleApplication(final ApplicationException e) {

        final HttpStatus status = resolveHttpStatus(e.getErrorCode());
        logAtLevel(status, "Application error: " + e.getMessage(), e);

        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(e.getErrorCode().getCode()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(final Exception e) {

        log.error("Unexpected error", e);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    private static HttpStatus resolveHttpStatus(final ErrorCode code) {

        return switch (code.getCategory()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static void logAtLevel(final HttpStatusCode status, final String message, final Exception e) {

        if (status.is5xxServerError()) {
            log.error(message, e);
        } else {
            log.warn(message);
        }
    }
}
