package com.rakesh.systemdesign.account.factory;

import com.rakesh.systemdesign.account.entity.*;
import com.rakesh.systemdesign.auth.entity.User;
import com.rakesh.systemdesign.exception.InvalidOperationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ==================== DESIGN PATTERN: FACTORY ====================
 *
 * WHAT:  A Factory creates objects WITHOUT exposing creation logic to the caller.
 *        The caller says: "I want a SAVINGS account" → Factory returns the right object.
 *
 * WHY NOT just use `new SavingsAccount()` directly?
 *
 *   Problem WITHOUT Factory:
 *     // In AccountService — scattered creation logic:
 *     if (type == SAVINGS) {
 *         SavingsAccount acc = new SavingsAccount();
 *         acc.setAccountNumber(generateNumber());
 *         acc.setInterestRate(new BigDecimal("4.0"));
 *         acc.setMinimumBalance(new BigDecimal("1000"));
 *         acc.setBalance(initialDeposit);
 *         acc.setUser(user);
 *         acc.setStatus(ACTIVE);
 *     } else if (type == CURRENT) {
 *         CurrentAccount acc = new CurrentAccount();
 *         acc.setAccountNumber(generateNumber());
 *         acc.setOverdraftLimit(new BigDecimal("50000"));
 *         // ... repeat similar code
 *     }
 *     // What if you add FIXED_DEPOSIT type later? Change every place that creates accounts!
 *
 *   Solution WITH Factory:
 *     Account account = accountFactory.createAccount(SAVINGS, user, initialDeposit);
 *     // ONE LINE. Factory handles all the details.
 *     // Add new type? Change ONLY the factory. Nothing else changes.
 *
 *   This is OPEN/CLOSED PRINCIPLE (SOLID - O):
 *     - Open for extension (add new account types)
 *     - Closed for modification (don't change existing code)
 *
 * NestJS comparison:
 *   In NestJS you might use a factory function or a provider factory:
 *     const accountFactory = {
 *       create(type: 'SAVINGS' | 'CURRENT', user, deposit) {
 *         switch(type) { case 'SAVINGS': return new SavingsAccount(...); }
 *       }
 *     };
 *
 * @Component → makes this a Spring Bean so it can be injected into AccountService.
 */
@Component
public class AccountFactory {

    /**
     * Creates the right type of Account based on AccountType.
     *
     * @param type           SAVINGS or CURRENT
     * @param user           The User who owns this account
     * @param initialDeposit Starting balance
     * @return               SavingsAccount or CurrentAccount (both are Account type)
     */
    public Account createAccount(AccountType type, User user, BigDecimal initialDeposit) {

        return switch (type) {
            case SAVINGS -> {
                SavingsAccount account = new SavingsAccount();
                account.setAccountNumber(generateAccountNumber());
                account.setBalance(initialDeposit);
                account.setUser(user);
                account.setStatus(AccountStatus.ACTIVE);
                // SavingsAccount-specific defaults are set in the entity class
                yield account;
            }
            case CURRENT -> {
                CurrentAccount account = new CurrentAccount();
                account.setAccountNumber(generateAccountNumber());
                account.setBalance(initialDeposit);
                account.setUser(user);
                account.setStatus(AccountStatus.ACTIVE);
                yield account;
            }
        };

        /*
         * FUTURE EXPANSION (Open/Closed Principle):
         *   When you add FIXED_DEPOSIT type:
         *     1. Create FixedDepositAccount extends Account
         *     2. Add FIXED_DEPOSIT to AccountType enum
         *     3. Add case here
         *   NOTHING else in the codebase needs to change!
         */
    }

    /**
     * Generates a unique account number.
     * Format: ACC-XXXXXXXX (8 random chars from UUID)
     *
     * In production: you'd use a sequence from DB for guaranteed uniqueness.
     */
    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
