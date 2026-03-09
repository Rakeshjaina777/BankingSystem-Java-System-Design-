package com.rakesh.systemdesign.account.entity;

import com.rakesh.systemdesign.auth.entity.User;
import com.rakesh.systemdesign.common.entity.BaseEntity;
import com.rakesh.systemdesign.exception.InsufficientBalanceException;
import com.rakesh.systemdesign.exception.InvalidOperationException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ==================== ACCOUNT ENTITY — OOP DEEP DIVE ====================
 *
 * This is where ALL major OOP concepts come together:
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                        BaseEntity                               │
 * │                   (id, createdAt, updatedAt)                    │
 * │                            ▲                                    │
 * │                            │ extends                            │
 * │                        Account                                  │
 * │           (accountNumber, balance, type, status)                │
 * │                      ▲              ▲                           │
 * │                      │              │ extends                   │
 * │              SavingsAccount    CurrentAccount                   │
 * │           (interestRate,      (overdraftLimit)                  │
 * │            minBalance)                                          │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * ==================== OOP CONCEPT 1: ENCAPSULATION ====================
 *
 * WHAT: Hiding internal data and controlling access through methods.
 *
 * PROBLEM WITHOUT ENCAPSULATION:
 *   account.setBalance(account.getBalance() + 1000);  // Anyone can set any balance!
 *   account.setBalance(-999999);  // Set negative balance? No validation!
 *
 * SOLUTION WITH ENCAPSULATION:
 *   account.deposit(1000);   // Goes through OUR method → validates → updates balance
 *   account.withdraw(500);   // Checks sufficient balance → checks account active → then updates
 *
 *   The balance field has NO public setter. The ONLY way to change balance is through
 *   deposit() and withdraw() methods. This is ENCAPSULATION.
 *
 *   NestJS comparison:
 *     In JS/TS you'd do: private _balance: number; with getter only.
 *     But JS doesn't enforce it at runtime. Java enforces at COMPILE TIME.
 *
 * ==================== OOP CONCEPT 2: INHERITANCE ====================
 *
 *   Account is the PARENT. SavingsAccount and CurrentAccount are CHILDREN.
 *   Children inherit ALL fields and methods from Account.
 *   Children can OVERRIDE methods to change behavior (Polymorphism).
 *
 * ==================== JPA INHERITANCE: SINGLE_TABLE strategy ====================
 *
 *   @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
 *
 *   3 JPA inheritance strategies:
 *
 *   1. SINGLE_TABLE (we use this):
 *      ONE table "accounts" stores ALL types.
 *      Extra column "account_type" distinguishes SAVINGS vs CURRENT.
 *      ✅ Fast queries (one table, no JOINs)
 *      ✅ Simple
 *      ❌ Unused columns for some types (savings has no overdraft_limit)
 *
 *   2. TABLE_PER_CLASS:
 *      Separate tables: "savings_accounts", "current_accounts"
 *      ❌ Slow for "find all accounts" (UNION query across tables)
 *
 *   3. JOINED:
 *      Base table "accounts" + child tables "savings_accounts", "current_accounts"
 *      ❌ Requires JOINs for every query
 *
 *   For banking: SINGLE_TABLE is best — fast queries, simple structure.
 *
 *   Resulting table in PostgreSQL:
 *   ┌──────┬────────────────┬─────────┬──────────┬────────┬───────────────┬─────────────────┬─────────┐
 *   │ id   │ account_number │ balance │ type     │ status │ interest_rate │ overdraft_limit │ user_id │
 *   ├──────┼────────────────┼─────────┼──────────┼────────┼───────────────┼─────────────────┼─────────┤
 *   │ uuid │ ACC-1001       │ 50000   │ SAVINGS  │ ACTIVE │ 4.0           │ NULL            │ uuid    │
 *   │ uuid │ ACC-1002       │ 100000  │ CURRENT  │ ACTIVE │ NULL          │ 50000           │ uuid    │
 *   └──────┴────────────────┴─────────┴──────────┴────────┴───────────────┴─────────────────┴─────────┘
 *
 * ==================== WHY BigDecimal (not double) for money? ====================
 *
 *   double: 0.1 + 0.2 = 0.30000000000000004  ← WRONG! Floating point error.
 *   BigDecimal: 0.1 + 0.2 = 0.3              ← CORRECT. Exact decimal math.
 *
 *   In BANKING, even ₹0.01 error matters. ALWAYS use BigDecimal for money.
 *   This is a PRODUCTION RULE — never use float/double for financial calculations.
 *
 * ==================== @ManyToOne: Account belongs to User ====================
 *
 *   One User can have MANY Accounts (1 savings + 1 current, or more).
 *   Many Accounts belong to ONE User.
 *
 *   @ManyToOne → "Many accounts belong to One user"
 *   @JoinColumn(name = "user_id") → "The foreign key column in accounts table is 'user_id'"
 *
 *   NestJS/TypeORM comparison:
 *     @ManyToOne(() => User)
 *     @JoinColumn({ name: 'user_id' })
 *     user: User;
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

    /**
     * ENCAPSULATION: balance has getter but controlled modification.
     * Use deposit() and withdraw() methods — NOT setBalance() directly.
     *
     * We can't remove the Lombok @Setter for just one field easily,
     * so we enforce the rule through code review and the methods below.
     * In production, you'd use @Setter(AccessLevel.NONE) on this field.
     */
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

    // ==================== ENCAPSULATED METHODS ====================

    /**
     * DEPOSIT money into account.
     *
     * WHY a method instead of setBalance()?
     *   1. Validates amount > 0
     *   2. Checks account is ACTIVE
     *   3. Updates balance atomically
     *   4. Single place for deposit logic — if rules change, change HERE only
     *
     * NestJS comparison:
     *   In NestJS you'd put this in a Service. In Java DDD (Domain-Driven Design),
     *   business logic that belongs to the ENTITY goes IN the entity.
     *   This is called a "Rich Domain Model" vs "Anemic Domain Model".
     */
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Deposit amount must be positive");
        }
        validateAccountActive();
        this.balance = this.balance.add(amount);
    }

    /**
     * WITHDRAW money from account.
     *
     * Checks:
     *   1. Amount must be positive
     *   2. Account must be ACTIVE
     *   3. Sufficient balance (for Savings — Current has overdraft, handled in subclass)
     */
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Withdrawal amount must be positive");
        }
        validateAccountActive();
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, this.balance);
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Check if account is active — reused by deposit() and withdraw().
     * Private helper method — not accessible from outside.
     */
    private void validateAccountActive() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "Account is " + this.status + ". Only ACTIVE accounts can perform transactions.");
        }
    }
}
