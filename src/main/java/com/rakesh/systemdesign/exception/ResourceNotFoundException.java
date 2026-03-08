package com.rakesh.systemdesign.exception;

/**
 * ==================== CUSTOM EXCEPTION ====================
 *
 * WHAT:  Thrown when we search for something in DB but it doesn't exist.
 *
 * HOW IT'S USED:
 *   throw new ResourceNotFoundException("User", "email", "rakesh@gmail.com");
 *   → Message: "User not found with email: rakesh@gmail.com"
 *
 *   throw new ResourceNotFoundException("Account", "id", "some-uuid");
 *   → Message: "Account not found with id: some-uuid"
 *
 * WHY extends RuntimeException (not Exception)?
 *   - RuntimeException = UNCHECKED → you don't need try-catch everywhere
 *   - Exception = CHECKED → Java FORCES you to write try-catch or throws in every method
 *   - Spring Boot convention: always use RuntimeException for business errors
 *
 * NestJS comparison:
 *   throw new NotFoundException('User not found');
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
    }
}
