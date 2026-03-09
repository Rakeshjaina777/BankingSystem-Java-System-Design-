package com.rakesh.systemdesign.account.repository;

import com.rakesh.systemdesign.account.entity.Account;
import com.rakesh.systemdesign.account.entity.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ==================== ACCOUNT REPOSITORY ====================
 *
 * Same pattern as UserRepository — interface only, Spring generates implementation.
 *
 * NEW CONCEPT: @Lock(PESSIMISTIC_WRITE) — used for transactions (explained below).
 *
 * Derived query method names:
 *   findByUser_Id(userId)
 *     → Spring reads: find + By + User + _ + Id
 *     → Generates: SELECT * FROM accounts WHERE user_id = ?
 *     → The underscore _ tells Spring to navigate the relationship: Account → User → id
 *
 *   findByUser_IdAndAccountType(userId, SAVINGS)
 *     → SELECT * FROM accounts WHERE user_id = ? AND account_type = ?
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Find all accounts belonging to a user.
     * One user can have multiple accounts (savings, current, etc.)
     */
    List<Account> findByUser_Id(UUID userId);

    /**
     * Check if a user already has a specific type of account.
     * We allow only ONE savings and ONE current per user.
     */
    boolean existsByUser_IdAndAccountType(UUID userId, AccountType accountType);

    /**
     * Find account by account number (the public-facing ID like "ACC-1A2B3C4D").
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * ==================== PESSIMISTIC LOCKING (CRITICAL FOR BANKING!) ====================
     *
     * WHAT: Locks the database ROW when reading it, so no other transaction can modify it
     *       until the current transaction completes.
     *
     * WHY THIS IS CRITICAL:
     *
     *   Scenario WITHOUT locking:
     *     Account balance: ₹10,000
     *     Thread 1 (withdraw ₹8000): reads balance = 10000
     *     Thread 2 (withdraw ₹8000): reads balance = 10000  ← SAME old value!
     *     Thread 1: 10000 - 8000 = 2000 → saves ₹2000
     *     Thread 2: 10000 - 8000 = 2000 → saves ₹2000
     *     RESULT: Both withdrawals succeed! Bank lost ₹6000! (should have only ₹2000 after first)
     *
     *   Scenario WITH pessimistic lock:
     *     Thread 1 (withdraw ₹8000): reads balance = 10000 + LOCKS the row 🔒
     *     Thread 2 (withdraw ₹8000): tries to read → BLOCKED! Waiting for lock... ⏳
     *     Thread 1: 10000 - 8000 = 2000 → saves ₹2000 → releases lock 🔓
     *     Thread 2: NOW reads balance = 2000 → 2000 < 8000 → INSUFFICIENT BALANCE! ✅
     *     RESULT: Correct! Only one withdrawal succeeds.
     *
     *   SQL generated:
     *     SELECT * FROM accounts WHERE id = ? FOR UPDATE
     *                                          ^^^^^^^^^^
     *                                     "Lock this row — nobody else can touch it"
     *
     * TWO TYPES OF LOCKS:
     *
     *   PESSIMISTIC_WRITE (we use this):
     *     - Locks the row on READ → nobody else can read OR write
     *     - Used when: you WILL modify the data (banking transactions)
     *     - Trade-off: slower (other threads wait), but SAFE
     *
     *   OPTIMISTIC (@Version annotation):
     *     - No lock on read → allows concurrent reads
     *     - On save, checks if version changed → if yes, throws exception
     *     - Used when: conflicts are RARE (profile updates, shopping cart)
     *     - Trade-off: faster, but needs retry logic for conflicts
     *
     *   For BANKING: ALWAYS use PESSIMISTIC. Money must NEVER be wrong.
     *
     * NestJS comparison:
     *   In TypeORM: queryRunner.manager.findOne(Account, { where: { id }, lock: { mode: 'pessimistic_write' } })
     *   Same concept — lock the row while you're working with it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);
}
