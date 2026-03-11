package com.rakesh.systemdesign.transaction.dto.response;

import com.rakesh.systemdesign.transaction.entity.Transaction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for all transaction types (deposit, withdrawal, transfer, payment).
 *
 * What the client receives after a transaction:
 * {
 *   "id": "uuid",
 *   "type": "TRANSFER",
 *   "amount": 5000.00,
 *   "status": "COMPLETED",
 *   "sourceAccountNumber": "ACC-1A2B3C4D",
 *   "targetAccountNumber": "ACC-5E6F7G8H",
 *   "balanceAfterSource": 15000.00,
 *   "balanceAfterTarget": 25000.00,
 *   "description": "Rent payment",
 *   "createdAt": "2026-03-10T10:30:00"
 * }
 */
@Getter
@Builder
public class TransactionResponse {

    private UUID id;
    private String idempotencyKey;
    private String type;
    private BigDecimal amount;
    private String status;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private BigDecimal balanceAfterSource;
    private BigDecimal balanceAfterTarget;
    private String description;
    private LocalDateTime createdAt;

    public static TransactionResponse fromEntity(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .idempotencyKey(txn.getIdempotencyKey())
                .type(txn.getType().name())
                .amount(txn.getAmount())
                .status(txn.getStatus().name())
                .sourceAccountNumber(txn.getSourceAccount() != null
                        ? txn.getSourceAccount().getAccountNumber()
                        : null)
                .targetAccountNumber(txn.getTargetAccount() != null
                        ? txn.getTargetAccount().getAccountNumber()
                        : null)
                .balanceAfterSource(txn.getBalanceAfterSource())
                .balanceAfterTarget(txn.getBalanceAfterTarget())
                .description(txn.getDescription())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
