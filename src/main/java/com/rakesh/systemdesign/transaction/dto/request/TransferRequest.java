package com.rakesh.systemdesign.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO for transferring money between two accounts.
 *
 * Client sends:
 * {
 *   "sourceAccountNumber": "ACC-1A2B3C4D",
 *   "targetAccountNumber": "ACC-5E6F7G8H",
 *   "amount": 5000,
 *   "idempotencyKey": "txn-uuid-here",
 *   "description": "Rent payment to landlord"
 * }
 */
@Getter
@Setter
public class TransferRequest {

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @NotBlank(message = "Target account number is required")
    private String targetAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    private String description;
}
