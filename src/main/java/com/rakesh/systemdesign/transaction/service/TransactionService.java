package com.rakesh.systemdesign.transaction.service;

import com.rakesh.systemdesign.account.entity.Account;
import com.rakesh.systemdesign.account.repository.AccountRepository;
import com.rakesh.systemdesign.exception.InvalidOperationException;
import com.rakesh.systemdesign.exception.ResourceNotFoundException;
import com.rakesh.systemdesign.transaction.dto.request.DepositRequest;
import com.rakesh.systemdesign.transaction.dto.request.TransferRequest;
import com.rakesh.systemdesign.transaction.dto.request.WithdrawRequest;
import com.rakesh.systemdesign.transaction.dto.response.TransactionResponse;
import com.rakesh.systemdesign.transaction.entity.Transaction;
import com.rakesh.systemdesign.transaction.entity.TransactionStatus;
import com.rakesh.systemdesign.transaction.entity.TransactionType;
import com.rakesh.systemdesign.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * ==================== TRANSACTION SERVICE — THE HEART OF BANKING ====================
 *
 * This is the MOST IMPORTANT class in the entire system.
 * Every rupee that moves goes through this service.
 *
 * KEY SAFETY MECHANISMS:
 *
 *   1. IDEMPOTENCY CHECK → prevents duplicate transactions
 *   2. PESSIMISTIC LOCKING → prevents race conditions (double-spend)
 *   3. @Transactional → all-or-nothing (atomicity)
 *   4. DEADLOCK PREVENTION → consistent lock ordering for transfers
 *   5. ENCAPSULATED METHODS → account.deposit()/withdraw() validate business rules
 *
 * ==================== HOW @Transactional WORKS HERE ====================
 *
 *   When you call transactionService.transfer():
 *
 *   Spring automatically does:
 *     BEGIN TRANSACTION;                    ← DB transaction starts
 *       SELECT * FROM accounts WHERE id=1 FOR UPDATE;  ← lock account A
 *       SELECT * FROM accounts WHERE id=2 FOR UPDATE;  ← lock account B
 *       UPDATE accounts SET balance=... WHERE id=1;     ← deduct from A
 *       UPDATE accounts SET balance=... WHERE id=2;     ← add to B
 *       INSERT INTO transactions (...) VALUES (...);    ← create record
 *     COMMIT;                               ← ALL changes saved at once
 *
 *   If ANY step throws an exception:
 *     ROLLBACK;                             ← ALL changes undone
 *     → Account A balance restored
 *     → Account B balance restored
 *     → No transaction record created
 *     → Money is SAFE
 *
 *   NestJS comparison:
 *     const queryRunner = connection.createQueryRunner();
 *     await queryRunner.startTransaction();
 *     try {
 *       // ... all operations ...
 *       await queryRunner.commitTransaction();
 *     } catch (err) {
 *       await queryRunner.rollbackTransaction();
 *     }
 *     In Spring: @Transactional does ALL of this automatically!
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    // ==================== DEPOSIT ====================

    /**
     * Deposit money INTO an account.
     *
     * Flow:
     *   1. Check idempotency (already processed?)
     *   2. Find account WITH LOCK (pessimistic)
     *   3. Call account.deposit(amount) → encapsulated validation + balance update
     *   4. Save account (updated balance)
     *   5. Create transaction record
     *   6. Return response
     *
     * WHY findByAccountNumberWithLock?
     *   Even for deposits, we lock the account row.
     *   Two concurrent deposits of ₹5000 each to same account:
     *
     *   WITHOUT lock:
     *     Thread 1: reads balance ₹10,000
     *     Thread 2: reads balance ₹10,000
     *     Thread 1: 10000+5000 = saves ₹15,000
     *     Thread 2: 10000+5000 = saves ₹15,000 ← OVERWRITES Thread 1's save!
     *     RESULT: Only ₹15,000 instead of ₹20,000. Bank LOST ₹5,000.
     *
     *   WITH lock:
     *     Thread 1: reads ₹10,000 + LOCKS 🔒
     *     Thread 2: WAITS... ⏳
     *     Thread 1: saves ₹15,000, releases 🔓
     *     Thread 2: reads ₹15,000 → saves ₹20,000 ✅
     */
    @Transactional
    public TransactionResponse deposit(DepositRequest request) {

        // Step 1: Idempotency check
        Optional<Transaction> existing = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return TransactionResponse.fromEntity(existing.get());
            // Already processed → return previous result, DON'T process again
        }

        // Step 2: Find account WITH LOCK
        Account account = accountRepository
                .findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account", "accountNumber", request.getAccountNumber()));

        // Step 3: Deposit (entity validates: amount > 0, account ACTIVE)
        account.deposit(request.getAmount());

        // Step 4: Save updated account
        accountRepository.save(account);

        // Step 5: Create transaction record
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .targetAccount(account)          // money goes INTO this account
                .balanceAfterTarget(account.getBalance())
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        return TransactionResponse.fromEntity(saved);
    }

    // ==================== WITHDRAWAL ====================

    /**
     * Withdraw money FROM an account.
     *
     * Same pattern as deposit, but:
     *   - sourceAccount (not target) — money goes OUT
     *   - account.withdraw() checks: sufficient balance, min balance (for savings)
     *   - Polymorphism in action: SavingsAccount.withdraw() vs CurrentAccount.withdraw()
     *     Spring/JPA loads the CORRECT subclass automatically!
     */
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {

        // Idempotency check
        Optional<Transaction> existing = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return TransactionResponse.fromEntity(existing.get());
        }

        // Find account WITH LOCK
        Account account = accountRepository
                .findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account", "accountNumber", request.getAccountNumber()));

        // Withdraw — entity handles validation (Encapsulation + Polymorphism)
        // If this is a SavingsAccount → checks minimumBalance
        // If this is a CurrentAccount → allows overdraft
        account.withdraw(request.getAmount());

        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .sourceAccount(account)          // money goes OUT of this account
                .balanceAfterSource(account.getBalance())
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        return TransactionResponse.fromEntity(saved);
    }

    // ==================== TRANSFER (most complex!) ====================

    /**
     * Transfer money from one account to another.
     *
     * ==================== DEADLOCK PREVENTION ====================
     *
     * WHAT IS A DEADLOCK?
     *   Thread 1: Transfer A → B. Locks A first, then tries to lock B.
     *   Thread 2: Transfer B → A. Locks B first, then tries to lock A.
     *
     *   Thread 1 holds A, waiting for B.
     *   Thread 2 holds B, waiting for A.
     *   → BOTH wait forever! This is a DEADLOCK.
     *
     *   ┌──────────┐         ┌──────────┐
     *   │ Thread 1 │──holds──│ Lock A   │──wants──→ Lock B (held by Thread 2)
     *   │ Thread 2 │──holds──│ Lock B   │──wants──→ Lock A (held by Thread 1)
     *   └──────────┘         └──────────┘
     *   → Both stuck forever ❌
     *
     * SOLUTION: Always lock accounts in CONSISTENT ORDER (by UUID).
     *
     *   Thread 1: Transfer A→B → Locks A first (smaller UUID), then B.
     *   Thread 2: Transfer B→A → Locks A first (smaller UUID), then B.
     *
     *   Now BOTH threads try to lock A first!
     *   Thread 1 gets A lock → proceeds.
     *   Thread 2 waits for A → no deadlock, just waiting.
     *
     *   Rule: compareUUID → lock the SMALLER UUID first, always.
     *   This is called "lock ordering" — a standard deadlock prevention technique.
     *
     * NestJS comparison:
     *   In NestJS you'd sort IDs before locking:
     *     const [first, second] = [accA, accB].sort((a, b) => a.id.localeCompare(b.id));
     *     await lockAndProcess(first, second);
     */
    @Transactional
    public TransactionResponse transfer(TransferRequest request) {

        // Cannot transfer to same account
        if (request.getSourceAccountNumber().equals(request.getTargetAccountNumber())) {
            throw new InvalidOperationException("Cannot transfer to the same account");
        }

        // Idempotency check
        Optional<Transaction> existing = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return TransactionResponse.fromEntity(existing.get());
        }

        // Find both accounts (without lock first — to get their IDs for ordering)
        Account sourceAccount = accountRepository
                .findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account", "accountNumber", request.getSourceAccountNumber()));

        Account targetAccount = accountRepository
                .findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account", "accountNumber", request.getTargetAccountNumber()));

        // DEADLOCK PREVENTION: Lock in consistent UUID order
        Account firstLock, secondLock;
        if (sourceAccount.getId().compareTo(targetAccount.getId()) < 0) {
            firstLock = accountRepository.findByIdWithLock(sourceAccount.getId()).orElseThrow();
            secondLock = accountRepository.findByIdWithLock(targetAccount.getId()).orElseThrow();
        } else {
            secondLock = accountRepository.findByIdWithLock(targetAccount.getId()).orElseThrow();
            firstLock = accountRepository.findByIdWithLock(sourceAccount.getId()).orElseThrow();
        }

        // Determine which is source and which is target after locking
        Account lockedSource = firstLock.getId().equals(sourceAccount.getId()) ? firstLock : secondLock;
        Account lockedTarget = firstLock.getId().equals(targetAccount.getId()) ? firstLock : secondLock;

        // Withdraw from source (entity validates: balance, active, min balance for savings)
        lockedSource.withdraw(request.getAmount());

        // Deposit to target (entity validates: active)
        lockedTarget.deposit(request.getAmount());

        // Save both accounts
        accountRepository.save(lockedSource);
        accountRepository.save(lockedTarget);

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .sourceAccount(lockedSource)
                .targetAccount(lockedTarget)
                .balanceAfterSource(lockedSource.getBalance())
                .balanceAfterTarget(lockedTarget.getBalance())
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        return TransactionResponse.fromEntity(saved);
    }

    // ==================== TRANSACTION HISTORY ====================

    /**
     * Get paginated transaction history for an account.
     *
     * @param accountId The account UUID
     * @param pageable  Contains page number, size, sort direction
     *                  Comes from URL: ?page=0&size=20&sort=createdAt,desc
     *                  Spring creates the Pageable object automatically!
     *
     * Page<T> vs List<T>:
     *   List<T>: just the data. No metadata.
     *   Page<T>: data + totalElements + totalPages + pageNumber + pageSize
     *
     *   .map(TransactionResponse::fromEntity)
     *     → Transforms Page<Transaction> into Page<TransactionResponse>
     *     → Each Transaction entity is converted to a DTO
     *     → Pagination metadata is preserved
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(UUID accountId, Pageable pageable) {
        // Verify account exists
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account", "id", accountId);
        }

        return transactionRepository.findByAccountId(accountId, pageable)
                .map(TransactionResponse::fromEntity);
    }
}
