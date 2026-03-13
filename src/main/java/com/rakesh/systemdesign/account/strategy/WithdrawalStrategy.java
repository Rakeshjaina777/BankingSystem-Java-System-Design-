package com.rakesh.systemdesign.account.strategy;

import java.math.BigDecimal;

/**
 * ==================== STRATEGY PATTERN: WithdrawalStrategy ====================
 *
 * WHAT:  An interface that defines HOW withdrawal validation works.
 *        Different implementations = different rules.
 *
 * WHY INTERFACE?
 *   - Account doesn't know WHICH strategy it uses
 *   - Strategy can be SWAPPED at runtime (VIP upgrade, festival offer)
 *   - New rules = new class implementing this interface (Open/Closed Principle)
 *
 * HOW IT WORKS:
 *   Account has: WithdrawalStrategy withdrawalStrategy
 *   Account.withdraw() calls: withdrawalStrategy.validate(balance, amount)
 *   The strategy decides: is this withdrawal allowed?
 *
 * BEFORE (Polymorphism):
 *   SavingsAccount.withdraw() → minBalance check hardcoded inside class
 *   CurrentAccount.withdraw() → overdraft check hardcoded inside class
 *   Can't change rules without creating new subclass!
 *
 * AFTER (Strategy):
 *   Account.withdraw() → delegates to withdrawalStrategy.validate()
 *   Swap strategy at runtime → change rules instantly!
 */
public interface WithdrawalStrategy {

    /**
     * Validates whether a withdrawal is allowed.
     * Throws exception if NOT allowed. Does nothing if allowed.
     *
     * @param balance Current account balance
     * @param amount  Amount to withdraw
     */
    void validate(BigDecimal balance, BigDecimal amount);
}
