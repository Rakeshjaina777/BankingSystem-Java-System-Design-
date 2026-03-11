package com.rakesh.systemdesign.transaction.repository;

import com.rakesh.systemdesign.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * ==================== TRANSACTION REPOSITORY ====================
 *
 * NEW CONCEPT: PAGINATION with Page<T> and Pageable
 *
 * WHY pagination?
 *   A user might have 10,000 transactions. Loading ALL at once:
 *     - Slow DB query (scanning all rows)
 *     - Huge memory usage (10,000 objects in RAM)
 *     - Slow network transfer (huge JSON response)
 *     - Bad user experience (loading forever)
 *
 *   WITH pagination: Load 20 at a time.
 *     Page 1: transactions 1-20
 *     Page 2: transactions 21-40
 *     ...
 *
 *   SQL generated:
 *     SELECT * FROM transactions WHERE source_account_id = ?
 *     ORDER BY created_at DESC
 *     LIMIT 20 OFFSET 0    ← page 0 (first 20)
 *     LIMIT 20 OFFSET 20   ← page 1 (next 20)
 *
 *   Spring handles this automatically with Pageable parameter.
 *
 *   Controller: GET /api/transactions/account/ACC-XXX?page=0&size=20&sort=createdAt,desc
 *     → Spring creates Pageable from query params automatically!
 *
 *   Page<Transaction> response includes:
 *     - content: the list of transactions
 *     - totalElements: total count across all pages
 *     - totalPages: how many pages
 *     - number: current page number
 *     - size: page size
 *
 * NestJS comparison:
 *   In TypeORM: this.txnRepo.find({ skip: 0, take: 20, order: { createdAt: 'DESC' } })
 *   In Spring: Spring does it from method signature + Pageable parameter.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find existing transaction by idempotency key.
     * Used to check: "Has this transaction already been processed?"
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Get transaction history for an account (as source OR target).
     *
     * JPQL (Java Persistence Query Language):
     *   - Like SQL but uses ENTITY names (Transaction, Account) not table names
     *   - :accountId is a named parameter, bound by @Param
     *
     * This query finds all transactions where:
     *   - The account is the SOURCE (money went OUT)
     *   - OR the account is the TARGET (money came IN)
     *
     * ORDER BY t.createdAt DESC → newest transactions first
     *
     * Pageable → Spring adds LIMIT/OFFSET automatically
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);
}
