package com.rakesh.systemdesign.auth.repository;

import com.rakesh.systemdesign.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * ==================== REPOSITORY: Database Access Layer ====================
 *
 * WHAT IS A REPOSITORY?
 *   It talks to the database. All SQL operations (SELECT, INSERT, UPDATE, DELETE)
 *   happen through the repository.
 *
 * THE MAGIC: You write NO implementation! Just an INTERFACE.
 *
 *   JpaRepository<User, UUID>
 *       ↑          ↑      ↑
 *       │          │      └── Type of the primary key (UUID)
 *       │          └── The Entity class this repository manages
 *       └── Spring's base repository with CRUD methods built-in
 *
 *   By extending JpaRepository, you automatically get these methods (for FREE):
 *
 *   ┌────────────────────────────────┬────────────────────────────────────────┐
 *   │ Method (you get for FREE)      │ SQL it generates                      │
 *   ├────────────────────────────────┼────────────────────────────────────────┤
 *   │ save(user)                     │ INSERT INTO users (...) VALUES (...)   │
 *   │ findById(uuid)                 │ SELECT * FROM users WHERE id = ?      │
 *   │ findAll()                      │ SELECT * FROM users                   │
 *   │ deleteById(uuid)               │ DELETE FROM users WHERE id = ?        │
 *   │ count()                        │ SELECT COUNT(*) FROM users            │
 *   │ existsById(uuid)              │ SELECT COUNT(*) > 0 FROM users WHERE..│
 *   └────────────────────────────────┴────────────────────────────────────────┘
 *
 *   You NEVER write these methods. Spring generates them at startup.
 *
 * CUSTOM QUERY METHODS (Derived Queries):
 *   Spring reads the METHOD NAME and generates SQL from it!
 *
 *   findByEmail("r@g.com")
 *     ↓ Spring reads: find + By + Email
 *     ↓ Generates: SELECT * FROM users WHERE email = 'r@g.com'
 *
 *   existsByEmail("r@g.com")
 *     ↓ Spring reads: exists + By + Email
 *     ↓ Generates: SELECT COUNT(*) > 0 FROM users WHERE email = 'r@g.com'
 *
 *   More examples (you can add these later):
 *     findByFullNameContaining("Rak")  → WHERE full_name LIKE '%Rak%'
 *     findByStatusAndEmail(status, email) → WHERE status = ? AND email = ?
 *
 * WHY Optional<User>?
 *   findByEmail might find a user OR might not.
 *   - Without Optional: returns null → NullPointerException risk!
 *   - With Optional: forces you to handle both cases explicitly.
 *
 *   Usage:
 *     Optional<User> maybeUser = userRepository.findByEmail("r@g.com");
 *     User user = maybeUser.orElseThrow(() -> new ResourceNotFoundException(...));
 *
 * NestJS/TypeORM comparison:
 *   @EntityRepository(User)
 *   export class UserRepository extends Repository<User> {
 *     findByEmail(email: string) {
 *       return this.findOne({ where: { email } });   // YOU write this
 *     }
 *   }
 *   In Spring → you just write the method SIGNATURE. Spring writes the implementation!
 *
 * WHERE IS THE CLASS THAT IMPLEMENTS THIS INTERFACE?
 *   There is NONE! Spring creates it at runtime using a Proxy.
 *   At startup: Spring sees "interface UserRepository extends JpaRepository"
 *   → Creates a proxy class implementing all methods
 *   → Registers it as a Bean in the IoC container
 *   → Injects it wherever @Autowired or constructor injection is used
 *
 *   This is the POWER of Spring. You declare WHAT you want, Spring handles HOW.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
