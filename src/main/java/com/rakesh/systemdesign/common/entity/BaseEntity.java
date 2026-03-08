package com.rakesh.systemdesign.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==================== OOP CONCEPT: INHERITANCE ====================
 *
 * WHAT:  BaseEntity is a PARENT class. All entities (User, Account, Transaction)
 *        EXTEND this class and automatically get id, createdAt, updatedAt.
 *
 * WHY:   Without this, EVERY entity would repeat these 3 fields.
 *        If you later add "createdBy" field → you add it in ONE place, all entities get it.
 *
 * NestJS comparison:
 *   In TypeORM you might do:  @CreateDateColumn() createdAt: Date;  in EVERY entity.
 *   Here we write it ONCE in BaseEntity → all entities inherit it.
 *
 * @MappedSuperclass → tells JPA: "Don't create a table for BaseEntity itself.
 *                      Just merge these columns into child entity tables."
 *
 * UUID vs Long for ID:
 *   - Long (auto-increment): 1, 2, 3... → Predictable. Attacker can guess other user IDs.
 *   - UUID: "550e8400-e29b-41d4-a716-446655440000" → Unpredictable. Secure for banking.
 *   - In production banking → ALWAYS use UUID for public-facing IDs.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
