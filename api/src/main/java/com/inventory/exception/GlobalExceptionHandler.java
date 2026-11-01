package com.inventory.exception;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.inventory.security.ProblemDetailWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/** Maps every exception to the RFC 9457 problem body. Never returns SQL, class names or stack traces. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemDetailWriter writer;

    public GlobalExceptionHandler(ProblemDetailWriter writer) {
        this.writer = writer;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> bodyValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiFieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiFieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, "Request validation failed.", request, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<Map<String, Object>> parameterValidation(HandlerMethodValidationException ex,
            HttpServletRequest request) {
        List<ApiFieldError> fields = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ApiFieldError(result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, "Request validation failed.", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Map<String, Object>> constraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<ApiFieldError> fields = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    return new ApiFieldError(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
                })
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, "Request validation failed.", request, fields);
    }

    @ExceptionHandler(RequestValidationException.class)
    ResponseEntity<Map<String, Object>> requestValidation(RequestValidationException ex, HttpServletRequest request) {
        return respond(ErrorCode.VALIDATION_FAILED, "Request validation failed.", request,
                List.of(new ApiFieldError(ex.field(), ex.getMessage())));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<Map<String, Object>> malformed(Exception ex, HttpServletRequest request) {
        return respond(ErrorCode.MALFORMED_REQUEST, "The request is malformed.", request, List.of());
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException ex, HttpServletRequest request) {
        return respond(ex.code(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        ConstraintViolationClassifier.Classification c = ConstraintViolationClassifier.classify(ex);
        ErrorCode code = switch (c.category()) {
            case DUPLICATE -> ErrorCode.DUPLICATE_RESOURCE;
            case STILL_REFERENCED -> ErrorCode.RESOURCE_IN_USE;
            case PARENT_MISSING -> ErrorCode.NOT_FOUND;
            case OTHER -> ErrorCode.INTERNAL_ERROR;
        };
        log.warn("Data integrity violation on {}: category={}, constraint={}, sqlState={}, vendorCode={}",
                request.getRequestURI(), c.category(), c.constraintName(), c.sqlState(), c.vendorCode());
        return respond(code, ConstraintViolationClassifier.clientMessage(c), request, List.of());
    }

    /** Other Spring MVC errors (unknown path, wrong HTTP method, ...): keep their status, safe message. */
    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class,
            ErrorResponseException.class})
    ResponseEntity<Map<String, Object>> springMvc(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(((ErrorResponse) ex).getStatusCode().value());
        ErrorCode code = status == HttpStatus.NOT_FOUND ? ErrorCode.NOT_FOUND
                : status.is4xxClientError() ? ErrorCode.MALFORMED_REQUEST : ErrorCode.INTERNAL_ERROR;
        String detail = ex instanceof HttpRequestMethodNotSupportedException ? "HTTP method not supported."
                : status == HttpStatus.NOT_FOUND ? "Resource not found." : "The request could not be processed.";
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(writer.body(status, code, detail, request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return respond(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred.", request, List.of());
    }

    private ResponseEntity<Map<String, Object>> respond(ErrorCode code, String detail, HttpServletRequest request,
            List<ApiFieldError> fieldErrors) {
        return ResponseEntity.status(code.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(writer.body(code.status(), code, detail, request.getRequestURI(), fieldErrors));
    }
}
