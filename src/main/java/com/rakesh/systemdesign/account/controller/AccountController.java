package com.rakesh.systemdesign.account.controller;

import com.rakesh.systemdesign.account.dto.request.CreateAccountRequest;
import com.rakesh.systemdesign.account.dto.response.AccountResponse;
import com.rakesh.systemdesign.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * ==================== ACCOUNT CONTROLLER ====================
 *
 * Endpoints:
 *   POST   /api/accounts          → Create new account
 *   GET    /api/accounts/{id}     → Get account by ID
 *   GET    /api/accounts/number/{accountNumber} → Get by account number
 *   GET    /api/accounts/user/{userId}          → Get all accounts for a user
 *
 * NEW ANNOTATIONS:
 *
 * @GetMapping("/{id}")
 *   → Maps GET /api/accounts/123-uuid to this method
 *   → {id} is a PATH VARIABLE — extracted from the URL
 *
 * @PathVariable UUID id
 *   → Takes the {id} from URL and converts it to UUID
 *   → NestJS comparison: @Param('id') id: string
 *
 *   Example:
 *     GET /api/accounts/550e8400-e29b-41d4-a716-446655440000
 *     → id = UUID("550e8400-e29b-41d4-a716-446655440000")
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Create and view bank accounts")
public class AccountController {

    private final AccountService accountService;

    /**
     * POST /api/accounts — Create a new bank account
     *
     * Request body:
     * {
     *   "userId": "uuid-here",
     *   "accountType": "SAVINGS",
     *   "initialDeposit": 5000
     * }
     */
    @Operation(summary = "Create a new bank account",
            description = "Creates a SAVINGS or CURRENT account for a user")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "409", description = "User already has this account type")
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/accounts/{id} — Get account by UUID
     *
     * @PathVariable → extracts {id} from URL path
     *
     * NestJS comparison:
     *   @Get(':id')
     *   async getAccount(@Param('id') id: string) { ... }
     */
    @Operation(summary = "Get account by ID")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    /**
     * GET /api/accounts/number/ACC-1A2B3C4D — Get account by account number
     */
    @Operation(summary = "Get account by account number")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    /**
     * GET /api/accounts/user/{userId} — Get all accounts for a user
     *
     * Returns a LIST of accounts (user can have savings + current).
     * ResponseEntity<List<AccountResponse>> → returns JSON array.
     */
    @Operation(summary = "Get all accounts for a user",
            description = "Returns all bank accounts belonging to the specified user")
    @ApiResponse(responseCode = "200", description = "Accounts found")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByUser(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }
}
