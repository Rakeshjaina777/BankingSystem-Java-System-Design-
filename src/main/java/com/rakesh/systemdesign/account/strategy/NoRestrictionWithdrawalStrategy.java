package com.rakesh.systemdesign.account.strategy;

import com.rakesh.systemdesign.exception.InsufficientBalanceException;

import java.math.BigDecimal;

/**
 * Strategy for VIP customers or special promotions.
 * Rule: just check basic balance >= amount. No minimum balance, no overdraft.
 *
 * USE CASES:
 *   - VIP savings customer: no minimum balance restriction
 *   - Festival season: temporarily remove restrictions
 *   - Closing account: withdraw full balance
 */
public class NoRestrictionWithdrawalStrategy implements WithdrawalStrategy {

    @Override
    public void validate(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, balance);
        }
    }
}
