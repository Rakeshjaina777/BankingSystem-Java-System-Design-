package com.rakesh.systemdesign.account.strategy;

import java.math.BigDecimal;

/**
 * ==================== STRATEGY PATTERN: InterestStrategy ====================
 *
 * Different accounts earn different interest rates.
 * Instead of hardcoding interest logic in each account subclass,
 * we plug in the right strategy.
 *
 * Savings  → RegularInterestStrategy (4%)
 * Current  → NoInterestStrategy (0%)
 * Future:  → SeniorCitizenInterestStrategy (6.5%)
 *          → FixedDepositInterestStrategy (7.5%)
 */
public interface InterestStrategy {

    BigDecimal calculateMonthlyInterest(BigDecimal balance);

    BigDecimal getAnnualRate();
}
