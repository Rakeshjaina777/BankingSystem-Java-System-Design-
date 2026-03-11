package com.rakesh.systemdesign.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO for depositing money into an account.
 *
 * Client sends:
 * {
 *   "accountNumber": "ACC-1A2B3C4D",
 *   "amount": 10000,
 *   "idempotencyKey": "dep-uuid-here",
 *   "description": "Salary credit"
 * }
 */
@Getter
@Setter
public class DepositRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    private String description;
}
