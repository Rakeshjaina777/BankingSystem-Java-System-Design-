package com.rakesh.systemdesign.auth.service;

import com.rakesh.systemdesign.auth.dto.request.LoginRequest;
import com.rakesh.systemdesign.auth.dto.request.SignupRequest;
import com.rakesh.systemdesign.auth.dto.response.UserResponse;
import com.rakesh.systemdesign.auth.entity.User;
import com.rakesh.systemdesign.auth.repository.UserRepository;
import com.rakesh.systemdesign.exception.DuplicateResourceException;
import com.rakesh.systemdesign.exception.ResourceNotFoundException;
import com.rakesh.systemdesign.exception.InvalidOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ==================== @Service CLASS ====================
 *
 * WHAT:  The Service layer contains ALL business logic.
 *        It sits between Controller (HTTP) and Repository (Database).
 *
 * RULE:  Controller → NEVER talks to Repository directly.
 *        Controller → calls Service → Service calls Repository.
 *
 *   Why? Single Responsibility Principle (S in SOLID):
 *     - Controller's job: handle HTTP request/response
 *     - Service's job: business rules and logic
 *     - Repository's job: database operations
 *
 * NestJS comparison:
 *   @Injectable()
 *   export class AuthService {
 *     constructor(
 *       private readonly userRepo: UserRepository,
 *       private readonly passwordEncoder: BcryptService,
 *     ) {}
 *   }
 *   Same pattern! @Injectable() = @Service, constructor injection = constructor injection.
 *
 * ==================== @RequiredArgsConstructor (Lombok) ====================
 *
 *   This generates a constructor with ALL `final` fields:
 *
 *   What Lombok generates at compile time:
 *     public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
 *         this.userRepository = userRepository;
 *         this.passwordEncoder = passwordEncoder;
 *     }
 *
 *   Spring sees this constructor → finds UserRepository bean and PasswordEncoder bean
 *   in the IoC container → injects them automatically.
 *
 *   WHY `final`?
 *     - final = this field MUST be set in constructor and can NEVER change after.
 *     - Guarantees: once AuthService is created, its dependencies can't be swapped.
 *     - This is called IMMUTABLE DEPENDENCY INJECTION — the safest way.
 *
 *   Without final:
 *     private UserRepository userRepository;  // Someone could do: authService.userRepository = null; → crash!
 *   With final:
 *     private final UserRepository userRepository;  // Compile error if you try to reassign.
 *
 * ==================== @Transactional ====================
 *
 *   WHAT: Wraps the method in a DATABASE TRANSACTION.
 *
 *   WHY IT MATTERS (Banking example):
 *     Transfer $100 from Account A to Account B:
 *       Step 1: Deduct $100 from A   ← succeeds
 *       Step 2: Add $100 to B        ← FAILS (server crash!)
 *
 *     WITHOUT @Transactional: $100 is gone from A, never added to B. Money lost!
 *     WITH @Transactional: Both steps succeed OR both roll back. No money lost.
 *
 *   @Transactional on signup: If saving user fails halfway, nothing is committed.
 *
 *   @Transactional(readOnly = true): For read-only operations (login, findUser).
 *     - Tells DB: "I won't modify data" → DB can optimize the query.
 *     - Hibernate won't do dirty-checking (performance boost).
 *
 *   NestJS comparison:
 *     In TypeORM: await queryRunner.startTransaction(); try { ... commit } catch { rollback }
 *     In Spring: Just add @Transactional → Spring handles begin/commit/rollback automatically!
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ==================== SIGNUP FLOW ====================
     *
     *  Step 1: Check if email already exists → throw 409 if yes
     *  Step 2: Hash the password (NEVER store plain text!)
     *  Step 3: Convert DTO → Entity
     *  Step 4: Save to database
     *  Step 5: Convert Entity → Response DTO (hide password)
     *
     *  Visual flow:
     *
     *  SignupRequest DTO          User Entity              UserResponse DTO
     *  ┌──────────────┐          ┌──────────────┐          ┌──────────────┐
     *  │ fullName     │  ──→     │ fullName     │  ──→     │ fullName     │
     *  │ email        │  ──→     │ email        │  ──→     │ email        │
     *  │ password     │  hash→   │ password     │  ✗       │              │
     *  │ phone        │  ──→     │ phone        │  ──→     │ phone        │
     *  │              │          │ id (auto)    │  ──→     │ id           │
     *  │              │          │ status (def) │  ✗       │              │
     *  │              │          │ createdAt    │  ──→     │ createdAt    │
     *  └──────────────┘          └──────────────┘          └──────────────┘
     *    (from client)           (saved in DB)             (sent to client)
     */
    @Transactional
    public UserResponse signup(SignupRequest request) {

        // Step 1: Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
            // GlobalExceptionHandler catches this → returns 409
        }

        // Step 2 & 3: Build User entity with hashed password
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())    // normalize email
                .password(passwordEncoder.encode(request.getPassword()))  // HASH password
                .phone(request.getPhone())
                // status defaults to ACTIVE (from @Builder.Default in User entity)
                .build();

        // Step 4: Save to DB
        // save() does: INSERT INTO users (id, full_name, email, password, ...) VALUES (...)
        // JPA auto-generates UUID for id, sets createdAt and updatedAt
        User savedUser = userRepository.save(user);

        // Step 5: Convert to response (hide password, internal fields)
        return UserResponse.fromEntity(savedUser);
    }

    /**
     * ==================== LOGIN FLOW ====================
     *
     *  Step 1: Find user by email → throw 404 if not found
     *  Step 2: Compare plain password with stored hash
     *  Step 3: Return user data (no JWT token yet — we'll add in later parts)
     *
     *  How password comparison works:
     *    User types: "myPassword123"
     *    DB has:     "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     *
     *    passwordEncoder.matches("myPassword123", "$2a$10$N9qo8u...")
     *      → BCrypt extracts salt from stored hash
     *      → Hashes "myPassword123" with that salt
     *      → Compares: does new hash == stored hash?
     *      → Returns true/false
     *
     *  NestJS comparison:
     *    const isMatch = await bcrypt.compare(loginDto.password, user.password);
     *    Same thing! Just different syntax.
     */
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {

        // Step 1: Find user by email
        // findByEmail returns Optional<User>
        // orElseThrow: if Optional is empty → throw the exception
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Step 2: Compare passwords
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Intentionally vague message — don't tell attacker whether email or password is wrong
            throw new InvalidOperationException("Invalid email or password");
        }

        // Step 3: Return user data
        return UserResponse.fromEntity(user);
    }
}
