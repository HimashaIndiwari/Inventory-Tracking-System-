package com.inventory.exception;

/** Validation failure detected in service code (e.g. from > to); reported as 400 with one field error. */
public class RequestValidationException extends ApiException {

    private final String field;

    public RequestValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
