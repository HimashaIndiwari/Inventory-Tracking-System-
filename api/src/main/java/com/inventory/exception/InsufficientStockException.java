package com.inventory.exception;

public class InsufficientStockException extends ApiException {

    public InsufficientStockException(int requested, long available) {
        super(ErrorCode.INSUFFICIENT_STOCK,
                "Insufficient stock: requested " + requested + ", available " + available + ".");
    }

    /** For ADJUST, where the message must say the adjustment itself would make stock negative. */
    public InsufficientStockException(String message) {
        super(ErrorCode.INSUFFICIENT_STOCK, message);
    }
}
