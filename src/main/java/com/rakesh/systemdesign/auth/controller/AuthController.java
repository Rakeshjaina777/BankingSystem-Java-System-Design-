package com.rakesh.systemdesign.auth.controller;

import com.rakesh.systemdesign.auth.dto.request.LoginRequest;
import com.rakesh.systemdesign.auth.dto.request.SignupRequest;
import com.rakesh.systemdesign.auth.dto.response.UserResponse;
import com.rakesh.systemdesign.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ==================== @RestController ====================
 *
 * WHAT:  Marks this class as an HTTP endpoint handler.
 *        Spring creates a Bean of this class and maps HTTP requests to its methods.
 *
 *   @RestController = @Controller + @ResponseBody
 *     - @Controller → "I handle HTTP requests"
 *     - @ResponseBody → "My return values are JSON, not HTML views"
 *
 *   NestJS comparison:
 *     @Controller('auth')
 *     export class AuthController { ... }
 *     Same thing! A class that handles HTTP routes.
 *
 * ==================== @RequestMapping("/api/auth") ====================
 *
 * WHAT:  Base URL prefix for ALL endpoints in this controller.
 *        Every method's URL starts with "/api/auth".
 *
 *   @PostMapping("/signup")  → full URL: POST /api/auth/signup
 *   @PostMapping("/login")   → full URL: POST /api/auth/login
 *
 *   WHY "/api" prefix?
 *     - Convention: API endpoints start with /api
 *     - Separates API routes from static files, health checks, etc.
 *     - In production, reverse proxy (Nginx) can route /api/* to your backend.
 *
 *   NestJS comparison:
 *     @Controller('api/auth')  → same thing
 *
 * ==================== CONTROLLER RULES (IMPORTANT!) ====================
 *
 *   1. Controller should be THIN — minimal code.
 *   2. Controller does ONLY:
 *      - Receive HTTP request
 *      - Validate input (@Valid)
 *      - Call service method
 *      - Return HTTP response
 *   3. Controller NEVER:
 *      - Contains business logic (that's Service's job)
 *      - Talks to Repository directly (that's Service's job)
 *      - Does calculations, validations beyond DTO, or data transformation
 *
 *   WHY? Single Responsibility Principle (SOLID - S):
 *     Controller = HTTP layer ONLY
 *     Service = Business logic ONLY
 *     Repository = Database ONLY
 *
 *   If you put everything in Controller:
 *     - Can't reuse logic (what if you need the same logic from a scheduled job?)
 *     - Can't test business logic without HTTP
 *     - One giant file that does everything → hard to maintain
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Signup and Login endpoints")
public class AuthController {

    private final AuthService authService;
    // Spring injects AuthService bean here (constructor injection via @RequiredArgsConstructor)

    /**
     * ==================== SIGNUP ENDPOINT ====================
     *
     * Full URL: POST http://localhost:8081/api/auth/signup
     *
     * @PostMapping("/signup")
     *   → "When someone sends HTTP POST to /api/auth/signup, run THIS method"
     *
     *   HTTP Methods recap:
     *     GET    → Read data     (get user, get accounts)
     *     POST   → Create data   (signup, create account)
     *     PUT    → Update data   (update profile — full replace)
     *     PATCH  → Partial update (change just email)
     *     DELETE → Delete data   (close account)
     *
     *   NestJS comparison:
     *     @Post('signup')
     *     async signup(@Body() signupDto: SignupDto) { ... }
     *
     * ==================== @Valid ====================
     *
     *   Triggers validation on the DTO BEFORE the method runs.
     *
     *   Flow:
     *     Client sends: { "email": "", "password": "12" }
     *                          ↓
     *     Spring sees @Valid → checks SignupRequest annotations:
     *       - @NotBlank on email → "" is blank → FAIL
     *       - @Size(min=6) on password → "12" is 2 chars → FAIL
     *                          ↓
     *     Spring throws MethodArgumentNotValidException
     *                          ↓
     *     GlobalExceptionHandler catches it → returns 400:
     *     { "status": 400, "errors": { "email": "Email is required", "password": "Password must be..." } }
     *
     *   WITHOUT @Valid: No validation happens. Empty email goes to service → crashes at DB level.
     *   WITH @Valid: Catches bad input EARLY, returns clean error message.
     *
     * ==================== @RequestBody ====================
     *
     *   "Take the JSON from the HTTP request body and convert it into a Java object"
     *
     *   Client sends JSON:        Spring converts to Java object:
     *   {                          SignupRequest request = new SignupRequest();
     *     "fullName": "Rakesh",    request.setFullName("Rakesh");
     *     "email": "r@g.com",     request.setEmail("r@g.com");
     *     "password": "123456"     request.setPassword("123456");
     *   }
     *
     *   This is called DESERIALIZATION (JSON → Java object).
     *   Jackson library does this automatically.
     *
     *   NestJS comparison: @Body() signupDto: SignupDto → same thing
     *
     * ==================== ResponseEntity<UserResponse> ====================
     *
     *   ResponseEntity wraps:
     *     1. BODY → the JSON data (UserResponse)
     *     2. STATUS CODE → 201 Created
     *     3. HEADERS → (optional, e.g., Location header)
     *
     *   Why not just return UserResponse directly?
     *     - You CAN: public UserResponse signup(...) → always returns 200 OK
     *     - With ResponseEntity: you CONTROL the status code
     *     - Signup should return 201 CREATED, not 200 OK
     *
     *   NestJS comparison:
     *     @HttpCode(201)
     *     @Post('signup')
     *     async signup(@Body() dto) { return this.authService.signup(dto); }
     */
    @Operation(summary = "Register a new user", description = "Creates a new user with hashed password")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse response = authService.signup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);   // 201 Created
    }

    /**
     * ==================== LOGIN ENDPOINT ====================
     *
     * Full URL: POST http://localhost:8081/api/auth/login
     *
     * Returns 200 OK with user data on success.
     * Returns 404 if email not found (ResourceNotFoundException).
     * Returns 400 if password wrong (InvalidOperationException).
     */
    @Operation(summary = "Login user", description = "Authenticates user with email and password")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "400", description = "Invalid password")
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        UserResponse response = authService.login(request);
        return ResponseEntity.ok(response);    // 200 OK
        // ResponseEntity.ok(body) is shorthand for: new ResponseEntity<>(body, HttpStatus.OK)
    }
}
