package com.rakesh.systemdesign.transaction.controller;

import com.rakesh.systemdesign.transaction.dto.request.DepositRequest;
import com.rakesh.systemdesign.transaction.dto.request.TransferRequest;
import com.rakesh.systemdesign.transaction.dto.request.WithdrawRequest;
import com.rakesh.systemdesign.transaction.dto.response.TransactionResponse;
import com.rakesh.systemdesign.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ==================== TRANSACTION CONTROLLER ====================
 *
 * Endpoints:
 *   POST /api/transactions/deposit    → Deposit money
 *   POST /api/transactions/withdraw   → Withdraw money
 *   POST /api/transactions/transfer   → Transfer between accounts
 *   GET  /api/transactions/account/{accountId} → Transaction history (paginated)
 *
 * NEW CONCEPT: @PageableDefault
 *
 *   When the client calls GET /api/transactions/account/{id} WITHOUT specifying
 *   page/size, Spring uses these defaults:
 *     - page = 0 (first page)
 *     - size = 20 (20 items per page)
 *     - sort = createdAt DESC (newest first)
 *
 *   Client CAN override:
 *     GET /api/transactions/account/{id}?page=2&size=10&sort=amount,asc
 *     → Page 3 (0-indexed), 10 items, sorted by amount ascending
 *
 *   Spring creates the Pageable object AUTOMATICALLY from these query params.
 *   You don't parse them yourself!
 *
 *   NestJS comparison:
 *     @Query('page') page: number = 0,
 *     @Query('size') size: number = 20,
 *     In Spring → @PageableDefault does it all in one annotation.
 *
 * ==================== WHY ALL TRANSACTIONS ARE POST (not PUT/PATCH)? ====================
 *
 *   POST = "Create a new resource"
 *   Every transaction CREATES a new transaction record.
 *   We never UPDATE or PATCH a transaction — financial records are IMMUTABLE.
 *
 *   Even "transfer" is a POST because it creates a new transaction.
 *   This is banking standard — once money moves, the record is permanent.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Deposit, Withdraw, Transfer, and Transaction History")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/transactions/deposit
     *
     * Request:
     * {
     *   "accountNumber": "ACC-1A2B3C4D",
     *   "amount": 10000,
     *   "idempotencyKey": "dep-uuid-here",
     *   "description": "Salary credit"
     * }
     *
     * Response: 201 Created
     * {
     *   "id": "txn-uuid",
     *   "type": "DEPOSIT",
     *   "amount": 10000,
     *   "status": "COMPLETED",
     *   "targetAccountNumber": "ACC-1A2B3C4D",
     *   "balanceAfterTarget": 60000.00,
     *   "description": "Salary credit"
     * }
     */
    @Operation(summary = "Deposit money into an account")
    @ApiResponse(responseCode = "201", description = "Deposit successful")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "400", description = "Invalid amount or account not active")
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * POST /api/transactions/withdraw
     *
     * Request:
     * {
     *   "accountNumber": "ACC-1A2B3C4D",
     *   "amount": 5000,
     *   "idempotencyKey": "wit-uuid-here",
     *   "description": "ATM withdrawal"
     * }
     *
     * Possible errors:
     *   - 400: Insufficient balance (Savings: balance - amount < minBalance)
     *   - 400: Account not active (FROZEN or CLOSED)
     *   - 404: Account not found
     */
    @Operation(summary = "Withdraw money from an account",
            description = "Savings: checks minimum balance. Current: allows overdraft.")
    @ApiResponse(responseCode = "201", description = "Withdrawal successful")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "400", description = "Insufficient balance or account not active")
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request) {
        TransactionResponse response = transactionService.withdraw(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * POST /api/transactions/transfer
     *
     * Request:
     * {
     *   "sourceAccountNumber": "ACC-1A2B3C4D",
     *   "targetAccountNumber": "ACC-5E6F7G8H",
     *   "amount": 5000,
     *   "idempotencyKey": "txn-uuid-here",
     *   "description": "Rent payment to landlord"
     * }
     *
     * This is the most COMPLEX endpoint:
     *   - Locks BOTH accounts (deadlock-safe ordering)
     *   - Withdraws from source (polymorphic — savings vs current rules)
     *   - Deposits to target
     *   - Creates transaction record
     *   - All-or-nothing (@Transactional)
     */
    @Operation(summary = "Transfer money between accounts",
            description = "Locks both accounts, prevents deadlocks, all-or-nothing atomic transfer")
    @ApiResponse(responseCode = "201", description = "Transfer successful")
    @ApiResponse(responseCode = "404", description = "Source or target account not found")
    @ApiResponse(responseCode = "400", description = "Insufficient balance, same account, or account not active")
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/transactions/account/{accountId}?page=0&size=20&sort=createdAt,desc
     *
     * Returns PAGINATED transaction history for an account.
     *
     * ==================== HOW PAGINATION WORKS ====================
     *
     * URL query parameters → Spring auto-creates Pageable:
     *
     *   ?page=0           → first page (0-indexed)
     *   &size=20          → 20 items per page
     *   &sort=createdAt,desc → sort by createdAt descending (newest first)
     *
     * @PageableDefault → default values if client doesn't specify:
     *   page=0, size=20, sort=createdAt descending
     *
     * Response (Page<TransactionResponse>):
     * {
     *   "content": [                          ← the actual transactions
     *     { "id": "...", "type": "DEPOSIT", "amount": 10000, ... },
     *     { "id": "...", "type": "WITHDRAWAL", "amount": 5000, ... }
     *   ],
     *   "pageable": {
     *     "pageNumber": 0,                    ← current page
     *     "pageSize": 20                      ← items per page
     *   },
     *   "totalElements": 150,                 ← total transactions across all pages
     *   "totalPages": 8,                      ← 150 / 20 = 8 pages
     *   "first": true,                        ← is this the first page?
     *   "last": false                         ← is this the last page?
     * }
     *
     * Frontend can use totalPages to render pagination:
     *   [ 1 ] [ 2 ] [ 3 ] ... [ 8 ] [ Next → ]
     *
     * NestJS comparison:
     *   @Query('page') page = 0, @Query('size') size = 20
     *   const [data, total] = await this.repo.findAndCount({ skip: page*size, take: size });
     *   return { data, total, totalPages: Math.ceil(total/size) };
     *   In Spring → ALL of this is handled by Page<T> automatically!
     */
    @Operation(summary = "Get transaction history for an account",
            description = "Paginated. Use ?page=0&size=20&sort=createdAt,desc")
    @ApiResponse(responseCode = "200", description = "Transaction history retrieved")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountId, pageable));
    }
}
