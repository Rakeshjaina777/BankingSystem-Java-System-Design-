package com.rakesh.systemdesign.account.strategy;

import com.rakesh.systemdesign.exception.InsufficientBalanceException;

import java.math.BigDecimal;

/**
 * Strategy for CURRENT accounts.
 * Rule: balance + overdraftLimit >= withdrawal amount.
 * Balance CAN go NEGATIVE up to -overdraftLimit.
 *
 * Example: balance=₹10,000, overdraft=₹50,000
 *   withdraw(48000) → 10000+50000=60000 >= 48000 → ✅ ALLOWED (balance becomes -38000)
 *   withdraw(70000) → 10000+50000=60000 <  70000 → ❌ DENIED
 */
public class OverdraftWithdrawalStrategy implements WithdrawalStrategy {

    private final BigDecimal overdraftLimit;

    public OverdraftWithdrawalStrategy(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public void validate(BigDecimal balance, BigDecimal amount) {
        BigDecimal availableFunds = balance.add(overdraftLimit);
        if (availableFunds.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, availableFunds);
        }
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
}
