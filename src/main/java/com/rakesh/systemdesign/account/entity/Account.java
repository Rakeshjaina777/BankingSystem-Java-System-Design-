package com.rakesh.systemdesign.account.entity;

import com.rakesh.systemdesign.account.strategy.InterestStrategy;
import com.rakesh.systemdesign.account.strategy.WithdrawalStrategy;
import com.rakesh.systemdesign.auth.entity.User;
import com.rakesh.systemdesign.common.entity.BaseEntity;
import com.rakesh.systemdesign.exception.InsufficientBalanceException;
import com.rakesh.systemdesign.exception.InvalidOperationException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ==================== ACCOUNT ENTITY — NOW WITH STRATEGY PATTERN ====================
 *
 * BEFORE (Polymorphism only):
 *   Account.withdraw()         → basic balance check
 *   SavingsAccount.withdraw()  → @Override with minBalance check (hardcoded)
 *   CurrentAccount.withdraw()  → @Override with overdraft check (hardcoded)
 *   Problem: Can't change rules at runtime. VIP upgrade? New subclass needed!
 *
 * AFTER (Strategy Pattern):
 *   Account.withdraw() → delegates to withdrawalStrategy.validate()
 *   Account.calculateMonthlyInterest() → delegates to interestStrategy
 *   SavingsAccount → still exists (for JPA discriminator + DB fields)
 *                     but NO @Override withdraw() — strategy handles it
 *   CurrentAccount → still exists (for JPA discriminator + DB fields)
 *                     but NO @Override withdraw() — strategy handles it
 *
 * WHAT CHANGED:
 *   - Added: WithdrawalStrategy field (pluggable withdrawal rules)
 *   - Added: InterestStrategy field (pluggable interest calculation)
 *   - Changed: withdraw() now DELEGATES to strategy instead of direct check
 *   - Added: calculateMonthlyInterest() in Account (was only in SavingsAccount)
 *
 * WHAT DID NOT CHANGE:
 *   - SavingsAccount / CurrentAccount classes still exist (JPA needs them)
 *   - Database table structure stays THE SAME
 *   - All controllers, services, DTOs → ZERO changes
 *   - deposit() → unchanged (same rules for all types)
 *
 * @Transient = "Don't save this field to database"
 *   Strategies are Java objects (not DB columns). They're assigned by Factory
 *   or by JPA @PostLoad callback after reading from DB.
 */
@Entity
@Table(name = "accounts")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "account_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", insertable = false, updatable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ==================== STRATEGY FIELDS ====================

    /**
     * @Transient → NOT stored in database. Assigned at runtime by:
     *   1. AccountFactory (when creating new account)
     *   2. @PostLoad in SavingsAccount/CurrentAccount (when reading from DB)
     *
     * WHY @Transient?
     *   Strategy is a Java OBJECT with behavior (methods).
     *   Database stores DATA (numbers, strings), not objects.
     *   The strategy is RECONSTRUCTED from DB data (interestRate, overdraftLimit columns).
     */
    @Transient
    private WithdrawalStrategy withdrawalStrategy;

    @Transient
    private InterestStrategy interestStrategy;

    // ==================== ENCAPSULATED METHODS (now with Strategy) ====================

    /**
     * DEPOSIT — unchanged. Same logic for ALL account types.
     */
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Deposit amount must be positive");
        }
        validateAccountActive();
        this.balance = this.balance.add(amount);
    }

    /**
     * WITHDRAW — now DELEGATES to strategy!
     *
     * BEFORE:
     *   Account.withdraw() → basic check: balance >= amount
     *   SavingsAccount.withdraw() → @Override: balance - amount >= minBalance
     *   CurrentAccount.withdraw() → @Override: balance + overdraft >= amount
     *
     * AFTER:
     *   Account.withdraw() → strategy.validate(balance, amount)
     *   Strategy decides the rules. Account doesn't know or care WHICH rules.
     *
     * The flow is now:
     *   1. Check amount > 0
     *   2. Check account is ACTIVE
     *   3. DELEGATE to strategy → strategy validates (minBalance? overdraft? no restriction?)
     *   4. Deduct balance
     *
     * If no strategy is set (safety fallback), uses basic balance >= amount check.
     */
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Withdrawal amount must be positive");
        }
        validateAccountActive();

        if (withdrawalStrategy != null) {
            // STRATEGY PATTERN: delegate validation to the plugged-in strategy
            withdrawalStrategy.validate(this.balance, amount);
        } else {
            // Fallback: basic check if no strategy assigned
            if (this.balance.compareTo(amount) < 0) {
                throw new InsufficientBalanceException(amount, this.balance);
            }
        }

        this.balance = this.balance.subtract(amount);
    }

    /**
     * CALCULATE INTEREST — now delegates to InterestStrategy.
     *
     * BEFORE: Only SavingsAccount had this method. CurrentAccount couldn't call it.
     * AFTER:  ALL accounts have this method. Strategy decides the result.
     *   Savings → RegularInterestStrategy → returns ₹166.67
     *   Current → NoInterestStrategy → returns ₹0
     */
    public BigDecimal calculateMonthlyInterest() {
        if (interestStrategy != null) {
            return interestStrategy.calculateMonthlyInterest(this.balance);
        }
        return BigDecimal.ZERO;  // default: no interest
    }

    private void validateAccountActive() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "Account is " + this.status + ". Only ACTIVE accounts can perform transactions.");
        }
    }
}
