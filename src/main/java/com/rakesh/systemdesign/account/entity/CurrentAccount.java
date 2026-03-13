package com.rakesh.systemdesign.account.entity;

import com.rakesh.systemdesign.account.strategy.NoInterestStrategy;
import com.rakesh.systemdesign.account.strategy.OverdraftWithdrawalStrategy;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ==================== CURRENT ACCOUNT — STRATEGY PATTERN VERSION ====================
 *
 * BEFORE (Polymorphism only):
 *   CurrentAccount had @Override withdraw() with hardcoded overdraft check.
 *   Problem: Can't change overdraft rules at runtime.
 *
 * AFTER (Strategy Pattern):
 *   CurrentAccount NO LONGER overrides withdraw().
 *   Instead, @PostLoad assigns strategies from DB fields:
 *     - OverdraftWithdrawalStrategy(overdraftLimit) → handles withdrawal with overdraft
 *     - NoInterestStrategy() → current accounts don't earn interest
 *
 * WHAT CHANGED:
 *   - REMOVED: @Override withdraw() → strategy handles it now
 *   - ADDED: @PostLoad initStrategies() → assigns strategies when JPA loads from DB
 *
 * WHAT DID NOT CHANGE:
 *   - overdraftLimit field → still in DB, still on this entity
 *   - @DiscriminatorValue("CURRENT") → JPA mapping unchanged
 *   - Database table → ZERO changes
 */
@Entity
@DiscriminatorValue("CURRENT")
@Getter
@Setter
@NoArgsConstructor
public class CurrentAccount extends Account {

    @Column(precision = 19, scale = 2)
    private BigDecimal overdraftLimit = new BigDecimal("50000");  // ₹50,000 overdraft

    /**
     * @PostLoad — Called by JPA AFTER loading this entity from database.
     *
     * Creates strategy objects from DB-stored field values:
     *   - overdraftLimit (DB column) → OverdraftWithdrawalStrategy (Java object)
     *   - No interest rate needed → NoInterestStrategy (always returns 0)
     */
    @PostLoad
    private void initStrategies() {
        setWithdrawalStrategy(new OverdraftWithdrawalStrategy(this.overdraftLimit));
        setInterestStrategy(new NoInterestStrategy());
    }

    // ==================== NO MORE @Override withdraw() ====================
    // Previously had:
    //   @Override
    //   public void withdraw(BigDecimal amount) { ... overdraft check ... }
    //
    // NOW: Account.withdraw() delegates to OverdraftWithdrawalStrategy.validate()
    //   which does the EXACT SAME check: balance + overdraftLimit >= amount
    //
    // The BEHAVIOR is identical. Only the STRUCTURE changed.
}
