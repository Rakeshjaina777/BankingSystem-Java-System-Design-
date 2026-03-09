package com.rakesh.systemdesign.account.entity;

import com.rakesh.systemdesign.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ==================== CurrentAccount — Overdraft Facility ====================
 *
 * WHAT IS OVERDRAFT?
 *   A Current Account can go INTO NEGATIVE balance up to a limit.
 *
 *   Example: balance = ₹2000, overdraftLimit = ₹50000
 *     - Can withdraw up to ₹52000 (2000 + 50000)
 *     - After withdrawing ₹52000: balance = -₹50000
 *     - Next deposit fills the overdraft first
 *
 *   Savings accounts CANNOT go negative. Current accounts CAN (up to the limit).
 *   This is WHY we need different classes — different RULES for each type.
 *
 * ==================== POLYMORPHISM IN ACTION ====================
 *
 *   Same method name, completely different logic:
 *
 *   SavingsAccount.withdraw(4500):
 *     "Is balance(5000) - 4500 >= minBalance(1000)?" → 500 < 1000 → DENIED!
 *
 *   CurrentAccount.withdraw(4500):
 *     "Is balance(2000) + overdraft(50000) >= 4500?" → 52000 >= 4500 → ALLOWED!
 *     New balance: 2000 - 4500 = -2500 (using overdraft)
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
     * Override withdraw to ALLOW overdraft.
     *
     * Parent withdraw: balance >= amount (no negative allowed)
     * Current withdraw: balance + overdraftLimit >= amount (negative allowed up to limit)
     */
    @Override
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.rakesh.systemdesign.exception.InvalidOperationException(
                    "Withdrawal amount must be positive");
        }
        // Check: can we cover this withdrawal including overdraft?
        BigDecimal availableFunds = getBalance().add(overdraftLimit);
        if (availableFunds.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, availableFunds);
        }
        // Directly subtract — balance CAN go negative (overdraft)
        setBalance(getBalance().subtract(amount));
    }
}
