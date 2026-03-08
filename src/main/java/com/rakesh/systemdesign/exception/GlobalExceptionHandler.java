package com.rakesh.systemdesign.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * ==================== GLOBAL EXCEPTION HANDLER ====================
 *
 * WHAT:  This class catches ALL exceptions thrown from ANY controller.
 *        It converts them into proper JSON error responses.
 *
 * HOW IT WORKS (the flow):
 *
 *   1. Controller calls Service
 *   2. Service throws: throw new ResourceNotFoundException("User", "email", "x@y.com")
 *   3. Exception bubbles UP from Service → Controller → Spring Framework
 *   4. Spring sees @RestControllerAdvice → checks if any @ExceptionHandler matches
 *   5. Finds handleResourceNotFound() → catches it → returns 404 JSON response
 *
 *   WITHOUT this class:
 *   → Spring returns ugly default error: { "timestamp": ..., "status": 500, "error": "Internal Server Error" }
 *   → No useful message, no proper status code.
 *
 *   WITH this class:
 *   → { "status": 404, "message": "User not found with email: x@y.com", "path": "/api/auth/login" }
 *
 * NestJS comparison:
 *   This is like @Catch() ExceptionFilter in NestJS:
 *
 *   @Catch(NotFoundException)
 *   export class NotFoundFilter implements ExceptionFilter {
 *     catch(exception, host) {
 *       const response = host.switchToHttp().getResponse();
 *       response.status(404).json({ message: exception.message });
 *     }
 *   }
 *
 *   But in Spring, @RestControllerAdvice is GLOBAL automatically — no need to register it.
 *
 * KEY ANNOTATIONS:
 *   @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 *     - @ControllerAdvice → "Apply this to ALL controllers"
 *     - @ResponseBody → "Return value is JSON, not a view/HTML"
 *
 *   @ExceptionHandler(SomeException.class) → "When SomeException is thrown, run THIS method"
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles: Resource not found (User, Account, Transaction)
     * HTTP Status: 404 NOT FOUND
     *
     * Example trigger: throw new ResourceNotFoundException("User", "email", "x@y.com")
     * Example response: { "status": 404, "message": "User not found with email: x@y.com" }
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request    // Spring injects the current HTTP request automatically
    ) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())      // 404
                .message(ex.getMessage())                   // "User not found with email: x@y.com"
                .path(request.getRequestURI())              // "/api/auth/login"
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        // ResponseEntity = HTTP response wrapper. Contains: body + status code + headers.
        // NestJS equivalent: res.status(404).json(error);
    }

    /**
     * Handles: Duplicate resource (email already exists)
     * HTTP Status: 409 CONFLICT
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())       // 409
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Handles: Insufficient balance for withdrawal/payment
     * HTTP Status: 400 BAD REQUEST
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())    // 400
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles: Invalid operations (e.g., deposit to closed account)
     * HTTP Status: 400 BAD REQUEST
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(
            InvalidOperationException ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles: DTO validation failures (@Valid)
     * HTTP Status: 400 BAD REQUEST
     *
     * When a request body fails validation, Spring throws MethodArgumentNotValidException.
     *
     * Example: SignupRequest has @NotBlank on email field.
     *   POST /api/auth/signup  { "email": "", "password": "123" }
     *   → This handler catches it → returns:
     *   {
     *     "status": 400,
     *     "message": "Validation failed",
     *     "errors": { "email": "Email is required", "password": "Password must be at least 6 characters" }
     *   }
     *
     * NestJS comparison:
     *   In NestJS, class-validator + ValidationPipe does this automatically.
     *   In Spring, @Valid + this handler does the same thing.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();

        // Loop through all field errors and collect them
        // getBindingResult() → contains all validation failures
        // getFieldErrors() → list of fields that failed validation
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.put(
                        fieldError.getField(),           // "email"
                        fieldError.getDefaultMessage()   // "Email is required"
                )
        );

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .errors(errors)
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles: ANY other unexpected exception (catch-all safety net)
     * HTTP Status: 500 INTERNAL SERVER ERROR
     *
     * This is the LAST resort. If no other handler matches, this one catches it.
     * In production, you'd also log this to monitoring (Datadog, Sentry, etc.)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())  // 500
                .message("An unexpected error occurred")
                // Don't expose actual exception message to client (security risk!)
                // Log it server-side instead
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
