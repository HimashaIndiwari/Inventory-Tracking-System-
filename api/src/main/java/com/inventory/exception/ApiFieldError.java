package com.inventory.exception;

/** One field-level validation problem (named to avoid clashing with Spring's FieldError). */
public record ApiFieldError(String field, String message) {
}
