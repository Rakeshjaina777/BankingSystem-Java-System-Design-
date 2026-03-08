package com.rakesh.systemdesign.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ==================== DESIGN PATTERN: BUILDER ====================
 *
 * WHAT:  @Builder (Lombok) generates a builder pattern for this class.
 *        Instead of: new ErrorResponse(404, "Not found", "/api/users", null, now)
 *        You write:  ErrorResponse.builder().status(404).message("Not found").build()
 *
 * WHY:   When a class has many fields, constructors become unreadable.
 *        Builder makes it clear WHICH field gets WHICH value.
 *
 * NestJS comparison:
 *   In NestJS you'd return: { statusCode: 404, message: 'Not found', timestamp: new Date() }
 *   Here we use a structured class so the error format is ALWAYS consistent.
 *
 * @JsonInclude(NON_NULL) → If a field is null, don't include it in JSON response.
 *        e.g., if there are no validation errors, "errors" key won't appear in response.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String message;
    private String path;
    private Map<String, String> errors;  // for validation errors (field → message)

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
