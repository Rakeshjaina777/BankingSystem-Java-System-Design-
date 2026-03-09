package com.rakesh.systemdesign.account.dto.request;

import com.rakesh.systemdesign.account.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a new bank account.
 *
 * Client sends:
 * {
 *   "userId": "uuid-of-the-user",
 *   "accountType": "SAVINGS",       ← or "CURRENT"
 *   "initialDeposit": 5000          ← opening balance
 * }
 *
 * @NotNull → field must be present (not null)
 * @DecimalMin("0") → deposit cannot be negative
 *
 * WHY userId and not the full User object?
 *   - Client should NOT send full User data to create an account
 *   - Client only knows the userId (from signup/login response)
 *   - Service will LOOK UP the User by userId from DB
 */
@Getter
@Setter
public class CreateAccountRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Account type is required (SAVINGS or CURRENT)")
    private AccountType accountType;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "0", message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;
}
