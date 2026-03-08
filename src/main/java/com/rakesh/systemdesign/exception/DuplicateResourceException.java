package com.rakesh.systemdesign.exception;

/**
 * Thrown when trying to create a resource that already exists.
 * e.g., Signup with an email that's already registered.
 *
 * NestJS comparison: throw new ConflictException('Email already exists');
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: %s", resourceName, fieldName, fieldValue));
    }
}
