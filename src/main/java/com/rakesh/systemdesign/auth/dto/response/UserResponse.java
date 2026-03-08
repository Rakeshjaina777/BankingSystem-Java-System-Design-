package com.rakesh.systemdesign.auth.dto.response;

import com.rakesh.systemdesign.auth.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==================== RESPONSE DTO ====================
 *
 * WHAT:  This is what the client RECEIVES after signup/login.
 *
 * WHY separate from User entity?
 *   - User entity has "password" field → you NEVER want to send password to client!
 *   - User entity has internal fields (updatedAt, status) client doesn't need.
 *   - Response DTO has ONLY what the client should see.
 *
 *   User Entity (in DB):                    UserResponse (sent to client):
 *   ┌──────────────────────┐                ┌──────────────────────┐
 *   │ id         ✓ send    │                │ id                   │
 *   │ fullName   ✓ send    │   ────────→    │ fullName             │
 *   │ email      ✓ send    │   (convert)    │ email                │
 *   │ password   ✗ NEVER   │                │ phone                │
 *   │ phone      ✓ send    │                │ createdAt            │
 *   │ status     ✗ skip    │                └──────────────────────┘
 *   │ createdAt  ✓ send    │
 *   │ updatedAt  ✗ skip    │
 *   └──────────────────────┘
 *
 * NestJS comparison:
 *   You might use class-transformer @Exclude() to hide password.
 *   Or create a separate response class. Same idea.
 *
 * STATIC FACTORY METHOD: fromEntity()
 *   Instead of writing conversion logic in the Service every time,
 *   we put it HERE. The DTO knows how to create itself from an Entity.
 *
 *   Usage: UserResponse.fromEntity(savedUser);
 *   Clean, reusable, single place to change if Entity fields change.
 */
@Getter
@Builder
public class UserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private LocalDateTime createdAt;

    /**
     * Converts a User ENTITY into a UserResponse DTO.
     *
     * This is a STATIC method — you call it on the CLASS, not on an object:
     *   UserResponse.fromEntity(user)    ✓ correct
     *   new UserResponse().fromEntity()  ✗ wrong
     *
     * NestJS comparison:
     *   static fromEntity(user: UserEntity): UserResponseDto {
     *     return { id: user.id, fullName: user.fullName, email: user.email };
     *   }
     */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
