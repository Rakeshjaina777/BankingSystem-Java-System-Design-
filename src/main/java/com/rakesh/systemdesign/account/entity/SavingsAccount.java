package com.rakesh.systemdesign.account.entity;

import com.rakesh.systemdesign.account.strategy.MinimumBalanceWithdrawalStrategy;
import com.rakesh.systemdesign.account.strategy.RegularInterestStrategy;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ==================== SAVINGS ACCOUNT — STRATEGY PATTERN VERSION ====================
 *
 * BEFORE (Polymorphism only):
 *   SavingsAccount had @Override withdraw() with hardcoded minBalance check.
 *   SavingsAccount had calculateMonthlyInterest() with hardcoded formula.
 *   Problem: Can't change rules at runtime. VIP upgrade? New subclass needed!
 *
 * AFTER (Strategy Pattern):
 *   SavingsAccount NO LONGER overrides withdraw() or calculateMonthlyInterest().
 *   Instead, @PostLoad assigns strategies from DB fields:
 *     - MinimumBalanceWithdrawalStrategy(minimumBalance) → handles withdrawal rules
 *     - RegularInterestStrategy(interestRate) → handles interest calculation
 *
 *   Account.withdraw() delegates to strategy.validate()
 *   Account.calculateMonthlyInterest() delegates to strategy.calculateMonthlyInterest()
 *
 * WHAT CHANGED:
 *   - REMOVED: @Override withdraw() → strategy handles it now
 *   - REMOVED: calculateMonthlyInterest() → strategy handles it now
 *   - ADDED: @PostLoad initStrategies() → assigns strategies when JPA loads from DB
 *
 * WHAT DID NOT CHANGE:
 *   - interestRate field → still in DB, still on this entity
 *   - minimumBalance field → still in DB, still on this entity
 *   - @DiscriminatorValue("SAVINGS") → JPA mapping unchanged
 *   - Database table → ZERO changes
 *
 * @PostLoad — JPA lifecycle callback:
 *   Called AUTOMATICALLY by JPA/Hibernate AFTER loading an entity from database.
 *   Perfect for initializing @Transient fields that can't be stored in DB.
 *
 *   Flow:
 *     1. JPA reads row from DB: { account_type: "SAVINGS", interest_rate: 4.0, min_balance: 1000 }
 *     2. JPA creates SavingsAccount object, sets all @Column fields
 *     3. JPA calls @PostLoad → initStrategies()
 *     4. initStrategies() creates strategy objects from the DB field values
 *     5. Now account.withdraw() and account.calculateMonthlyInterest() work via strategies!
 *
 *   NestJS comparison:
 *     In TypeORM you'd use @AfterLoad():
 *       @AfterLoad()
 *       initStrategies() {
 *         this.withdrawalStrategy = new MinimumBalanceWithdrawalStrategy(this.minimumBalance);
 *       }
 */
@Entity
@DiscriminatorValue("SAVINGS")
@Getter
@Setter
@NoArgsConstructor
public class SavingsAccount extends Account {

    @Column(precision = 5, scale = 2)
    private BigDecimal interestRate = new BigDecimal("4.0");   // 4% annual interest

    @Column(precision = 19, scale = 2)
    private BigDecimal minimumBalance = new BigDecimal("1000");  // ₹1000 minimum

    /**
     * @PostLoad — Called by JPA AFTER loading this entity from database.
     *
     * Creates strategy objects from the DB-stored field values.
     * This bridges the gap between "data in DB" and "behavior in Java":
     *   - minimumBalance (DB column) → MinimumBalanceWithdrawalStrategy (Java object)
     *   - interestRate (DB column) → RegularInterestStrategy (Java object)
     *
     * WHY here and not in Account?
     *   Account doesn't know about interestRate or minimumBalance fields.
     *   Only SavingsAccount has these columns → only SavingsAccount knows
     *   which strategies to create and with what parameters.
     */
    @PostLoad
    private void initStrategies() {
        setWithdrawalStrategy(new MinimumBalanceWithdrawalStrategy(this.minimumBalance));
        setInterestStrategy(new RegularInterestStrategy(this.interestRate));
    }

    // ==================== NO MORE @Override withdraw() ====================
    // Previously had:
    //   @Override
    //   public void withdraw(BigDecimal amount) { ... minBalance check ... }
    //
    // NOW: Account.withdraw() delegates to MinimumBalanceWithdrawalStrategy.validate()
    //   which does the EXACT SAME check: balance - amount >= minimumBalance
    //
    // The BEHAVIOR is identical. Only the STRUCTURE changed.
    // This gives us runtime flexibility (can swap to NoRestrictionWithdrawalStrategy for VIP).

    // ==================== NO MORE calculateMonthlyInterest() ====================
    // Previously had:
    //   public BigDecimal calculateMonthlyInterest() { ... formula ... }
    //
    // NOW: Account.calculateMonthlyInterest() delegates to RegularInterestStrategy
    //   which does the EXACT SAME formula: balance * rate / 1200
}
