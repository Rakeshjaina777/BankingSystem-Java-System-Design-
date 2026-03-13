package com.rakesh.systemdesign.account.strategy;

import com.rakesh.systemdesign.exception.InsufficientBalanceException;

import java.math.BigDecimal;

/**
 * Strategy for SAVINGS accounts.
 * Rule: balance AFTER withdrawal must stay >= minimumBalance.
 *
 * Example: balance=₹50,000, minBalance=₹1,000
 *   withdraw(49000) → 50000-49000=1000 >= 1000 → ✅ ALLOWED
 *   withdraw(49500) → 50000-49500=500  <  1000 → ❌ DENIED
 */
public class MinimumBalanceWithdrawalStrategy implements WithdrawalStrategy {

    private final BigDecimal minimumBalance;

    public MinimumBalanceWithdrawalStrategy(BigDecimal minimumBalance) {
        this.minimumBalance = minimumBalance;
    }

    @Override
    public void validate(BigDecimal balance, BigDecimal amount) {
        BigDecimal balanceAfter = balance.subtract(amount);
        if (balanceAfter.compareTo(minimumBalance) < 0) {
            BigDecimal available = balance.subtract(minimumBalance);
            throw new InsufficientBalanceException(amount, available);
            // "Insufficient balance. Requested: 49500, Available: 49000"
        }
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }
}
