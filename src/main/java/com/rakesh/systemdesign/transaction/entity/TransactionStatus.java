package com.rakesh.systemdesign.transaction.entity;

/**
 * ==================== TRANSACTION STATUS ====================
 *
 * Transaction lifecycle:
 *
 *   PENDING → transaction started but not yet completed
 *       ↓ success           ↓ failure
 *   COMPLETED            FAILED
 *
 * WHY track status?
 *   - If server crashes MID-TRANSACTION, the status stays PENDING
 *   - A scheduled job can detect PENDING transactions and retry/rollback them
 *   - This is called SAGA pattern in distributed systems
 *
 * For now: we set COMPLETED on success, FAILED on any exception.
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}
