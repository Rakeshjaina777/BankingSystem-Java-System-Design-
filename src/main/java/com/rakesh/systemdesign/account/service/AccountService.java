package com.rakesh.systemdesign.account.service;

import com.rakesh.systemdesign.account.dto.request.CreateAccountRequest;
import com.rakesh.systemdesign.account.dto.response.AccountResponse;
import com.rakesh.systemdesign.account.entity.Account;
import com.rakesh.systemdesign.account.factory.AccountFactory;
import com.rakesh.systemdesign.account.repository.AccountRepository;
import com.rakesh.systemdesign.auth.entity.User;
import com.rakesh.systemdesign.auth.repository.UserRepository;
import com.rakesh.systemdesign.exception.DuplicateResourceException;
import com.rakesh.systemdesign.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ==================== ACCOUNT SERVICE ====================
 *
 * Business logic for account operations:
 *   - Create account (uses Factory pattern)
 *   - Get account by ID
 *   - Get all accounts for a user
 *
 * Notice how this service is CLEAN and SHORT because:
 *   1. Entity handles its own validation (Encapsulation → deposit/withdraw methods)
 *   2. Factory handles object creation (Factory Pattern)
 *   3. Repository handles DB operations (Repository Pattern)
 *   4. Service just ORCHESTRATES the flow
 *
 * This is what good architecture looks like — each class has ONE job (SRP).
 *
 * DEPENDENCY INVERSION PRINCIPLE (SOLID - D):
 *   This service depends on:
 *     - AccountRepository (interface) → not the implementation
 *     - AccountFactory (abstraction) → not direct `new SavingsAccount()`
 *   If you swap PostgreSQL for MongoDB → change only the repository, service stays the same.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountFactory accountFactory;

    /**
     * Create a new bank account.
     *
     * Flow:
     *   1. Find the user (must exist)
     *   2. Check if user already has this account type (no duplicates)
     *   3. Factory creates the right account type
     *   4. Save to DB
     *   5. Return response DTO
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {

        // Step 1: Find user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        // Step 2: Check duplicate — one SAVINGS and one CURRENT per user
        if (accountRepository.existsByUser_IdAndAccountType(
                request.getUserId(), request.getAccountType())) {
            throw new DuplicateResourceException(
                    "Account", "type", request.getAccountType().name());
        }

        // Step 3: Factory creates account (Factory Pattern in action!)
        Account account = accountFactory.createAccount(
                request.getAccountType(),
                user,
                request.getInitialDeposit()
        );

        // Step 4: Save
        Account savedAccount = accountRepository.save(account);

        // Step 5: Convert to response
        return AccountResponse.fromEntity(savedAccount);
    }

    /**
     * Get account by ID.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        return AccountResponse.fromEntity(account);
    }

    /**
     * Get account by account number (e.g., "ACC-1A2B3C4D").
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account", "accountNumber", accountNumber));

        return AccountResponse.fromEntity(account);
    }

    /**
     * Get all accounts for a user.
     *
     * .stream() → converts List to a Stream (like array.map() in JS)
     * .map(AccountResponse::fromEntity) → converts each Account to AccountResponse
     *   This is a METHOD REFERENCE — shorthand for: account -> AccountResponse.fromEntity(account)
     * .toList() → collects back into a List
     *
     * NestJS comparison:
     *   const accounts = await this.accountRepo.find({ where: { userId } });
     *   return accounts.map(acc => AccountResponse.fromEntity(acc));
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUserId(UUID userId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return accountRepository.findByUser_Id(userId)
                .stream()
                .map(AccountResponse::fromEntity)
                .toList();
    }
}
