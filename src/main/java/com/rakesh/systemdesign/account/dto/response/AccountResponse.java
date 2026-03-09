package com.rakesh.systemdesign.account.dto.response;

import com.rakesh.systemdesign.account.entity.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO — what client sees after creating/viewing an account.
 *
 * Notice: NO user password, NO internal JPA fields.
 * Only what the client NEEDS to see.
 *
 * Static factory: AccountResponse.fromEntity(account)
 *   - Checks if account is SavingsAccount → includes interestRate, minimumBalance
 *   - Checks if account is CurrentAccount → includes overdraftLimit
 *
 * This uses INSTANCEOF pattern matching (Java 16+):
 *   if (account instanceof SavingsAccount sa) { ... }
 *   "sa" is automatically cast — no need for: SavingsAccount sa = (SavingsAccount) account;
 */
@Getter
@Builder
public class AccountResponse {

    private UUID id;
    private String accountNumber;
    private BigDecimal balance;
    private String accountType;
    private String status;
    private UUID userId;
    private String userName;
    private LocalDateTime createdAt;

    // Savings-specific fields (null for Current accounts)
    private BigDecimal interestRate;
    private BigDecimal minimumBalance;

    // Current-specific fields (null for Savings accounts)
    private BigDecimal overdraftLimit;

    /**
     * Converts Account entity → AccountResponse DTO.
     *
     * Uses instanceof to detect the ACTUAL type (Polymorphism at work):
     *   - Account variable could hold SavingsAccount or CurrentAccount object
     *   - instanceof checks the REAL type at runtime
     */
    public static AccountResponse fromEntity(Account account) {
        AccountResponseBuilder builder = AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .accountType(account.getAccountType() != null
                        ? account.getAccountType().name()
                        : account.getClass().getSimpleName())
                .status(account.getStatus().name())
                .userId(account.getUser().getId())
                .userName(account.getUser().getFullName())
                .createdAt(account.getCreatedAt());

        // Add type-specific fields
        if (account instanceof SavingsAccount sa) {
            builder.interestRate(sa.getInterestRate());
            builder.minimumBalance(sa.getMinimumBalance());
        } else if (account instanceof CurrentAccount ca) {
            builder.overdraftLimit(ca.getOverdraftLimit());
        }

        return builder.build();
    }
}
