package com.rakesh.systemdesign.account.entity;

import com.rakesh.systemdesign.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ==================== OOP CONCEPT 3: POLYMORPHISM ====================
 *
 * WHAT:  Same method name (withdraw), different behavior per class.
 *
 *   Account.withdraw(500)         → basic check: balance >= 500
 *   SavingsAccount.withdraw(500)  → extra check: balance - 500 >= minBalance (₹1000)
 *   CurrentAccount.withdraw(500)  → allows overdraft up to limit
 *
 *   The code calling withdraw() doesn't need to know which TYPE of account it is!
 *
 *     Account account = accountRepository.findById(id);  // Could be Savings or Current
 *     account.withdraw(500);  // Java calls the CORRECT version automatically!
 *
 *   This is RUNTIME POLYMORPHISM (method overriding).
 *
 * NestJS comparison:
 *   In TypeScript, you'd use interfaces or abstract classes:
 *     interface Account { withdraw(amount: number): void; }
 *     class SavingsAccount implements Account { withdraw(amount) { ... min balance check } }
 *     class CurrentAccount implements Account { withdraw(amount) { ... overdraft check } }
 *
 * @DiscriminatorValue("SAVINGS"):
 *   When JPA saves a SavingsAccount, it puts "SAVINGS" in the account_type column.
 *   When JPA reads a row with account_type="SAVINGS", it creates a SavingsAccount object.
 *
 * ==================== LISKOV SUBSTITUTION PRINCIPLE (SOLID - L) ====================
 *
 *   "Any code that works with Account should also work with SavingsAccount."
 *
 *   Example:
 *     public void processDeposit(Account account, BigDecimal amount) {
 *         account.deposit(amount);  // Works for ANY account type!
 *     }
 *
 *   You can pass SavingsAccount OR CurrentAccount — both work correctly.
 *   This is Liskov Substitution in action.
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
     * ==================== METHOD OVERRIDING (Polymorphism) ====================
     *
     * @Override means: "I'm REPLACING the parent's withdraw() with my own version."
     *
     * Parent (Account.withdraw):
     *   - Checks: amount > 0, account active, balance >= amount
     *
     * Child (SavingsAccount.withdraw):
     *   - All parent checks PLUS: balance after withdrawal >= minimumBalance
     *
     * WHY override?
     *   Savings accounts have a MINIMUM BALANCE rule.
     *   If balance is ₹5000 and min is ₹1000, max withdrawal = ₹4000 (not ₹5000).
     *
     * super.withdraw(amount):
     *   Calls the PARENT's withdraw method. We don't duplicate the parent's validation —
     *   we ADD our extra check, then let the parent do the rest.
     *   This is the OPEN/CLOSED PRINCIPLE (SOLID - O): extending without modifying.
     */
    @Override
    public void withdraw(BigDecimal amount) {
        BigDecimal balanceAfterWithdrawal = getBalance().subtract(amount);
        if (balanceAfterWithdrawal.compareTo(minimumBalance) < 0) {
            throw new InsufficientBalanceException(amount,
                    getBalance().subtract(minimumBalance));
            // "Insufficient balance. Requested: 4500, Available: 4000"
            // Available = balance - minBalance = 5000 - 1000 = 4000
        }
        super.withdraw(amount);  // Call parent's withdraw (does the actual balance deduction)
    }

    /**
     * Calculate interest for the account.
     * This method EXISTS ONLY in SavingsAccount, not in CurrentAccount.
     * CurrentAccount doesn't earn interest.
     *
     * Formula: balance * (interestRate / 100) / 12  → monthly interest
     */
    public BigDecimal calculateMonthlyInterest() {
        return getBalance()
                .multiply(interestRate)
                .divide(new BigDecimal("1200"), 2, java.math.RoundingMode.HALF_UP);
    }
}
