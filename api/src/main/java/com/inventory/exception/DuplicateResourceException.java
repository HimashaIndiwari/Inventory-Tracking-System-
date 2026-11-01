package com.inventory.exception;

public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String detail) {
        super(ErrorCode.DUPLICATE_RESOURCE, detail);
    }
}
