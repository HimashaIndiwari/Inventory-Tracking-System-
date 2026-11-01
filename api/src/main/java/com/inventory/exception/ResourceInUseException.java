package com.inventory.exception;

public class ResourceInUseException extends ApiException {

    public ResourceInUseException(String detail) {
        super(ErrorCode.RESOURCE_IN_USE, detail);
    }
}
