package com.rakesh.systemdesign.account.strategy;

import java.math.BigDecimal;

/**
 * No interest — for Current accounts.
 * Current accounts don't earn interest. Always returns 0.
 */
public class NoInterestStrategy implements InterestStrategy {

    @Override
    public BigDecimal calculateMonthlyInterest(BigDecimal balance) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getAnnualRate() {
        return BigDecimal.ZERO;
    }
}
