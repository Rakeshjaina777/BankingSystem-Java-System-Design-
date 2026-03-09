package com.rakesh.systemdesign.account.entity;

/**
 * Account lifecycle:
 *   ACTIVE → can do all operations (deposit, withdraw, payment)
 *   FROZEN → can only view, no transactions (e.g., suspicious activity)
 *   CLOSED → permanently closed, no operations allowed
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
