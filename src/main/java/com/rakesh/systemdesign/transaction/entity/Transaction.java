package com.rakesh.systemdesign.transaction.entity;

import com.rakesh.systemdesign.account.entity.Account;
import com.rakesh.systemdesign.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ==================== TRANSACTION ENTITY ====================
 *
 * Every financial operation (deposit, withdrawal, transfer, payment) creates
 * a Transaction record. This is the AUDIT TRAIL — never deleted, never modified.
 *
 * Table in PostgreSQL:
 * ┌──────┬────────────────┬──────────────┬──────────┬──────────┬──────────┬───────────┬────────────┐
 * │ id   │ idempotency_key│ type         │ amount   │ status   │source_id │ target_id │ description│
 * ├──────┼────────────────┼──────────────┼──────────┼──────────┼──────────┼───────────┼────────────┤
 * │ uuid │ pay-123-abc    │ TRANSFER     │ 5000.00  │ COMPLETED│ acc-uuid │ acc-uuid  │ Rent       │
 * │ uuid │ dep-456-def    │ DEPOSIT      │ 10000.00 │ COMPLETED│ NULL     │ acc-uuid  │ Salary     │
 * │ uuid │ wit-789-ghi    │ WITHDRAWAL   │ 2000.00  │ FAILED   │ acc-uuid │ NULL      │ ATM        │
 * └──────┴────────────────┴──────────────┴──────────┴──────────┴──────────┴───────────┴────────────┘
 *
 * SOURCE vs TARGET:
 *   DEPOSIT:    source = NULL,    target = account (money goes INTO target)
 *   WITHDRAWAL: source = account, target = NULL    (money goes OUT of source)
 *   TRANSFER:   source = A,       target = B       (money moves from A to B)
 *   PAYMENT:    source = account, target = NULL    (money goes OUT for a bill)
 *
 * ==================== IDEMPOTENCY KEY (CRITICAL!) ====================
 *
 * WHAT:  A unique identifier sent by the CLIENT to prevent DUPLICATE transactions.
 *
 * THE PROBLEM (without idempotency):
 *
 *   1. Client sends: POST /api/transactions/transfer { amount: 5000, key: null }
 *   2. Server processes transfer: A-5000, B+5000  ✅
 *   3. Network timeout — client doesn't receive response!
 *   4. Client RETRIES: POST /api/transactions/transfer { amount: 5000, key: null }
 *   5. Server processes AGAIN: A-5000, B+5000  ❌ DOUBLE TRANSFER!
 *   6. Account A lost ₹10,000 instead of ₹5,000!
 *
 * THE SOLUTION (with idempotency):
 *
 *   1. Client sends: POST /transfer { amount: 5000, idempotencyKey: "txn-abc-123" }
 *   2. Server: key "txn-abc-123" not found → process transfer → save with key
 *   3. Network timeout — client retries
 *   4. Client sends: POST /transfer { amount: 5000, idempotencyKey: "txn-abc-123" }
 *   5. Server: key "txn-abc-123" FOUND! → return previous result, DON'T process again
 *   6. Account A lost only ₹5,000  ✅ Correct!
 *
 * HOW CLIENT GENERATES THE KEY:
 *   - UUID: "550e8400-e29b-41d4-a716-446655440000" (random, unique)
 *   - Or: "transfer-{fromAcc}-{toAcc}-{timestamp}" (deterministic)
 *   - Frontend generates it, backend just checks for duplicates
 *
 * NestJS comparison:
 *   In NestJS you'd check: await this.txnRepo.findOne({ idempotencyKey })
 *   If found → return existing result. Same pattern.
 *
 * ==================== @Index ====================
 *
 * @Table(indexes = ...) creates a DATABASE INDEX on the idempotencyKey column.
 *
 * WHY: We query by idempotencyKey on EVERY transaction request.
 *   WITHOUT index: DB scans ALL rows to find the key → SLOW (O(n))
 *   WITH index: DB uses B-tree index to find instantly → FAST (O(log n))
 *
 *   Like a book index: instead of reading every page to find "Polymorphism",
 *   you look in the index → page 42 → go directly there.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    /**
     * Unique key to prevent duplicate transactions.
     * Sent by the client. If a transaction with this key already exists,
     * we return the existing one instead of creating a new one.
     */
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * Source account — money goes OUT of this account.
     * NULL for DEPOSIT (money comes from external source).
     *
     * @ManyToOne → Many transactions can belong to one account.
     * FetchType.LAZY → Don't load the full Account object until we actually need it.
     *   Without LAZY: every time you load a Transaction, it also loads the Account,
     *                 which loads the User, which loads... (N+1 query problem)
     *   With LAZY: Account is loaded ONLY when you call transaction.getSourceAccount()
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    /**
     * Target account — money goes INTO this account.
     * NULL for WITHDRAWAL and PAYMENT (money goes to external destination).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;

    @Column(length = 500)
    private String description;

    /**
     * Stores the balance of source account AFTER the transaction.
     * Useful for transaction history display:
     *   "Withdrew ₹5000 | Balance after: ₹15,000"
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfterSource;

    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfterTarget;
}
