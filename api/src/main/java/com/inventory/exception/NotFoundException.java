package com.inventory.exception;

public class NotFoundException extends ApiException {

    public NotFoundException(String detail) {
        super(ErrorCode.NOT_FOUND, detail);
    }
}
