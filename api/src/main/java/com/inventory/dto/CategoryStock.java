package com.inventory.dto;

import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Exact per-category stock total; serialised as a decimal string (may exceed Long.MAX_VALUE). */
public record CategoryStock(String categoryName, @JsonFormat(shape = JsonFormat.Shape.STRING) BigInteger totalQuantity) {
}
