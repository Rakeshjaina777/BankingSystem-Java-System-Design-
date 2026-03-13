package com.rakesh.systemdesign.account.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regular interest for Savings accounts.
 * Formula: balance * (annualRate / 100) / 12 = monthly interest
 *
 * Example: balance=₹50,000, rate=4%
 *   50000 * 4.0 / 1200 = ₹166.67 per month
 */
public class RegularInterestStrategy implements InterestStrategy {

    private final BigDecimal annualRate;

    public RegularInterestStrategy(BigDecimal annualRate) {
        this.annualRate = annualRate;
    }

    @Override
    public BigDecimal calculateMonthlyInterest(BigDecimal balance) {
        return balance
                .multiply(annualRate)
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getAnnualRate() {
        return annualRate;
    }
}
