package com.rakesh.systemdesign.auth.entity;

import com.rakesh.systemdesign.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ==================== ENTITY CLASS: User ====================
 *
 * WHAT IS AN ENTITY?
 *   - An Entity = a Java class that maps to a DATABASE TABLE.
 *   - Each FIELD in the class = a COLUMN in the table.
 *   - Each OBJECT of this class = a ROW in the table.
 *
 *   Class  →  Table
 *   Field  →  Column
 *   Object →  Row
 *
 *   Example:
 *     User class → "users" table in PostgreSQL
 *     User object { name="Rakesh", email="r@g.com" } → one row in "users" table
 *
 * NestJS/TypeORM comparison:
 *
 *   @Entity()
 *   export class User {
 *     @PrimaryGeneratedColumn('uuid') id: string;
 *     @Column({ unique: true }) email: string;
 *     @Column() password: string;
 *   }
 *
 *   In Java Spring, it's the SAME idea — just different annotations.
 *
 * ==================== INHERITANCE IN ACTION ====================
 *
 *   User extends BaseEntity
 *     ↓
 *   User automatically gets: id (UUID), createdAt, updatedAt
 *   You don't write them again! They come from the parent class.
 *
 *   Final "users" table in PostgreSQL:
 *   ┌──────────┬──────────────┬──────────────────┬────────────┬────────────┬────────────┐
 *   │ id (UUID)│ full_name    │ email            │ password   │ created_at │ updated_at │
 *   ├──────────┼──────────────┼──────────────────┼────────────┼────────────┼────────────┤
 *   │ abc-123  │ Rakesh Kumar │ rakesh@gmail.com │ $2a$10$... │ 2026-03-08 │ 2026-03-08 │
 *   └──────────┴──────────────┴──────────────────┴────────────┴────────────┴────────────┘
 *
 * ==================== ANNOTATIONS EXPLAINED ====================
 *
 * @Entity       → "This class maps to a database table"
 * @Table        → "The table name is 'users'" (without this, table name = class name = "User")
 *                  We use "users" because "user" is a RESERVED WORD in PostgreSQL.
 *
 * @Getter/@Setter → Lombok generates getters/setters for ALL fields.
 *   Without Lombok:
 *     public String getFullName() { return this.fullName; }
 *     public void setFullName(String name) { this.fullName = name; }
 *   With Lombok: You write NOTHING. @Getter/@Setter does it automatically at compile time.
 *
 * @NoArgsConstructor → Generates: public User() { }
 *   JPA REQUIRES a no-arg constructor to create entity objects from DB rows.
 *   When JPA reads a row from DB → it does: User user = new User(); user.setFullName("Rakesh");
 *
 * @AllArgsConstructor → Generates: public User(String fullName, String email, String password, ...) { }
 *   Useful when YOU create User objects in code.
 *
 * @Builder → Generates builder pattern (explained in ErrorResponse).
 *   User.builder().fullName("Rakesh").email("r@g.com").password("hashed").build();
 *
 * @Column annotations:
 *   - nullable = false → This column CANNOT be NULL in database (like NOT NULL in SQL)
 *   - unique = true → This column must have UNIQUE values (no two users with same email)
 *   - length = 100 → Maximum 100 characters (VARCHAR(100) in SQL)
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;   // Will store HASHED password, never plain text

    @Column(length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)   // Store enum as STRING in DB, not number
    @Column(nullable = false)
    @Builder.Default               // When using builder, default to ACTIVE
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * ==================== ENUM ====================
     *
     * WHAT: An enum is a fixed set of constants. UserStatus can ONLY be ACTIVE or INACTIVE.
     *       You can't do: user.setStatus("BANANA") → compile error!
     *
     * WHY @Enumerated(EnumType.STRING)?
     *   - EnumType.ORDINAL (default) → stores as number: ACTIVE=0, INACTIVE=1
     *     Problem: If you add SUSPENDED between ACTIVE and INACTIVE later,
     *              all existing INACTIVE rows (stored as 1) now become SUSPENDED!
     *   - EnumType.STRING → stores as text: "ACTIVE", "INACTIVE"
     *     Safe: Adding new values doesn't break existing data.
     *
     * NestJS comparison:
     *   export enum UserStatus { ACTIVE = 'ACTIVE', INACTIVE = 'INACTIVE' }
     *   @Column({ type: 'enum', enum: UserStatus, default: UserStatus.ACTIVE })
     */
    public enum UserStatus {
        ACTIVE,
        INACTIVE
    }
}
