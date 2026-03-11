package com.rakesh.systemdesign.transaction.entity;

/**
 * ==================== TRANSACTION TYPES ====================
 *
 * DEPOSIT:   Money IN to an account (ATM deposit, salary credit)
 *            → Only target account involved
 *
 * WITHDRAWAL: Money OUT from an account (ATM withdrawal)
 *            → Only source account involved
 *
 * TRANSFER:  Money from one account to another (A → B)
 *            → BOTH source and target accounts involved
 *            → This is the most complex — needs BOTH accounts locked
 *
 * PAYMENT:   Money out for a service (bill payment, merchant)
 *            → Like withdrawal but categorized differently
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    PAYMENT
}
