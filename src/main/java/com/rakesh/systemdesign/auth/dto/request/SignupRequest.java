package com.rakesh.systemdesign.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * ==================== DTO: Data Transfer Object ====================
 *
 * WHAT IS A DTO?
 *   A DTO is a SIMPLE class that carries data between layers.
 *   It is NOT an Entity. It does NOT map to a database table.
 *
 * WHY USE DTOs? (Why not just use User entity directly?)
 *
 *   Problem without DTO:
 *     POST /api/auth/signup  body: { "fullName": "Rakesh", "email": "r@g.com", "password": "123" }
 *     If controller accepts User entity directly →
 *       - Client can send "id", "createdAt", "status" and OVERRIDE your values!
 *       - You expose your database structure to the outside world.
 *       - Security risk: client can set status = "ADMIN" if you had a role field.
 *
 *   Solution with DTO:
 *     SignupRequest has ONLY the fields the client SHOULD send.
 *     You control exactly what comes in and what goes out.
 *
 *   Flow:  Client sends JSON → Spring converts to SignupRequest DTO
 *                             → Service converts DTO to User Entity
 *                             → Repository saves Entity to DB
 *                             → Service converts Entity to UserResponse DTO
 *                             → Controller returns UserResponse DTO as JSON
 *
 *   NestJS comparison:
 *     export class SignupDto {
 *       @IsNotEmpty() fullName: string;
 *       @IsEmail() email: string;
 *       @MinLength(6) password: string;
 *     }
 *     Same concept! DTO + validation decorators.
 *
 * ==================== VALIDATION ANNOTATIONS ====================
 *
 *   @NotBlank  → Field must not be null AND must not be empty/whitespace
 *                " " → fails. "" → fails. null → fails. "Rakesh" → passes.
 *
 *   @Email     → Must be valid email format (contains @, has domain, etc.)
 *
 *   @Size(min=6, max=100) → String length must be between 6 and 100 characters.
 *
 *   These annotations do NOTHING by themselves!
 *   They only work when controller uses @Valid:
 *     public ResponseEntity signup(@Valid @RequestBody SignupRequest request) { }
 *                                  ^^^^^^
 *                                  This triggers validation
 *
 *   If validation fails → Spring throws MethodArgumentNotValidException
 *   → Our GlobalExceptionHandler catches it → returns 400 with field errors.
 */
@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    private String phone;   // optional field — no @NotBlank
}
