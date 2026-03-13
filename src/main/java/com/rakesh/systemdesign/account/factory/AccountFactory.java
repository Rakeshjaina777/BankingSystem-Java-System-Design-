package com.rakesh.systemdesign.account.factory;

import com.rakesh.systemdesign.account.entity.*;
import com.rakesh.systemdesign.account.strategy.*;
import com.rakesh.systemdesign.auth.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ==================== ACCOUNT FACTORY — NOW WITH STRATEGY ASSIGNMENT ====================
 *
 * BEFORE:
 *   Factory created SavingsAccount/CurrentAccount objects.
 *   Strategies didn't exist — subclasses had hardcoded @Override methods.
 *
 * AFTER:
 *   Factory creates objects AND assigns the correct strategies.
 *   This is important because @PostLoad only runs when loading FROM database.
 *   For NEWLY CREATED accounts (not yet saved to DB), Factory must assign strategies.
 *
 * TWO PATHS for strategy assignment:
 *   1. NEW account → Factory assigns strategies (this class)
 *   2. EXISTING account loaded from DB → @PostLoad assigns strategies (in entity classes)
 *
 *   Both paths result in the same strategies being assigned.
 *   This ensures withdraw() and calculateMonthlyInterest() ALWAYS work,
 *   whether the account was just created or loaded from database.
 *
 * NestJS comparison:
 *   Same concept — when creating a new object, you'd set the strategy:
 *     const account = new SavingsAccount();
 *     account.withdrawalStrategy = new MinimumBalanceWithdrawalStrategy(1000);
 *     account.interestStrategy = new RegularInterestStrategy(4.0);
 */
@Component
public class AccountFactory {

    /**
     * Creates the right type of Account based on AccountType.
     * NOW also assigns the correct withdrawal and interest strategies.
     *
     * @param type           SAVINGS or CURRENT
     * @param user           The User who owns this account
     * @param initialDeposit Starting balance
     * @return               SavingsAccount or CurrentAccount with strategies assigned
     */
    public Account createAccount(AccountType type, User user, BigDecimal initialDeposit) {

        return switch (type) {
            case SAVINGS -> {
                SavingsAccount account = new SavingsAccount();
                account.setAccountNumber(generateAccountNumber());
                account.setBalance(initialDeposit);
                account.setUser(user);
                account.setStatus(AccountStatus.ACTIVE);

                // STRATEGY ASSIGNMENT — for newly created account
                // Uses the default values from SavingsAccount fields:
                //   interestRate = 4.0, minimumBalance = 1000
                account.setWithdrawalStrategy(
                        new MinimumBalanceWithdrawalStrategy(account.getMinimumBalance()));
                account.setInterestStrategy(
                        new RegularInterestStrategy(account.getInterestRate()));

                yield account;
            }
            case CURRENT -> {
                CurrentAccount account = new CurrentAccount();
                account.setAccountNumber(generateAccountNumber());
                account.setBalance(initialDeposit);
                account.setUser(user);
                account.setStatus(AccountStatus.ACTIVE);

                // STRATEGY ASSIGNMENT — for newly created account
                // Uses the default value from CurrentAccount field:
                //   overdraftLimit = 50000
                account.setWithdrawalStrategy(
                        new OverdraftWithdrawalStrategy(account.getOverdraftLimit()));
                account.setInterestStrategy(new NoInterestStrategy());

                yield account;
            }
        };

        /*
         * FUTURE EXPANSION (Open/Closed Principle):
         *   When you add FIXED_DEPOSIT type:
         *     1. Create FixedDepositAccount extends Account
         *     2. Add FIXED_DEPOSIT to AccountType enum
         *     3. Add case here with appropriate strategies
         *     4. Create new strategy if needed (e.g. PenaltyWithdrawalStrategy)
         *   NOTHING else in the codebase needs to change!
         */
    }

    /**
     * Generates a unique account number.
     * Format: ACC-XXXXXXXX (8 random chars from UUID)
     */
    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
