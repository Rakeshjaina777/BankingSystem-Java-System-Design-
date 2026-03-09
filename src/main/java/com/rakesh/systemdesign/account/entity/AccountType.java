package com.rakesh.systemdesign.account.entity;

/**
 * ==================== ENUM: AccountType ====================
 *
 * Two types of bank accounts — each behaves differently:
 *
 *   SAVINGS:
 *     - Earns interest (e.g., 4% per year)
 *     - Has minimum balance requirement (e.g., ₹1000)
 *     - Limited withdrawals per month
 *
 *   CURRENT:
 *     - No interest
 *     - No minimum balance
 *     - Unlimited transactions
 *     - Has overdraft facility (can go negative up to a limit)
 *
 * WHY separate types?
 *   Different RULES for each type = different BEHAVIOR.
 *   This is where OOP Polymorphism shines — same method name (withdraw),
 *   different implementation per type.
 */
public enum AccountType {
    SAVINGS,
    CURRENT
}
