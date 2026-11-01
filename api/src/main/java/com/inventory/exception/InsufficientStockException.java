package com.inventory.exception;

public class InsufficientStockException extends ApiException {

    public InsufficientStockException(int requested, long available) {
        super(ErrorCode.INSUFFICIENT_STOCK,
                "Insufficient stock: requested " + requested + ", available " + available + ".");
    }
}
