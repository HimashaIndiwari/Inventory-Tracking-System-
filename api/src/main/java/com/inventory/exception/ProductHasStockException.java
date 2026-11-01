package com.inventory.exception;

public class ProductHasStockException extends ApiException {

    public ProductHasStockException(long quantity) {
        super(ErrorCode.PRODUCT_HAS_STOCK, "Product still has " + quantity
                + " units in stock. Record a Stock OUT for the remaining quantity before archiving.");
    }
}
