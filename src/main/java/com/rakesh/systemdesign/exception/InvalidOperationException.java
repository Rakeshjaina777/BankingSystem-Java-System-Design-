package com.rakesh.systemdesign.exception;

/**
 * Thrown when an operation is not allowed.
 * e.g., Trying to deposit to a CLOSED account.
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
