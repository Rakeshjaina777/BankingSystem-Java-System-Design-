package com.rakesh.systemdesign.exception;

import java.math.BigDecimal;

/**
 * Thrown when a withdrawal/payment exceeds the available balance.
 * Banking-specific exception.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient balance. Requested: %s, Available: %s", requested, available));
    }
}
