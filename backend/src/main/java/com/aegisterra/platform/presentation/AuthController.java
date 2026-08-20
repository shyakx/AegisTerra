package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.identity.AuthService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.AuthUserResponse;
import com.aegisterra.platform.application.contracts.ChangePasswordRequest;
import com.aegisterra.platform.application.contracts.ForgotPasswordRequest;
import com.aegisterra.platform.application.contracts.LoginRequest;
import com.aegisterra.platform.application.contracts.LoginResponse;
import com.aegisterra.platform.application.contracts.MessageResponse;
import com.aegisterra.platform.application.contracts.ResetPasswordRequest;
import com.aegisterra.platform.application.contracts.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Cookie-based JWT authentication and password flows")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Validates credentials and sets HttpOnly access/refresh cookies.")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(authService.login(request, httpRequest, httpResponse));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Rotates the refresh token and issues a new access cookie.")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(authService.refresh(httpRequest, httpResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the refresh token family and clears auth cookies.")
    public ResponseEntity<MessageResponse> logout(
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse,
        @AuthenticationPrincipal AegisUserPrincipal principal
    ) {
        authService.logout(httpRequest, httpResponse, principal);
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(new MessageResponse("Logged out"));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user")
    public ResponseEntity<AuthUserResponse> me(@AuthenticationPrincipal AegisUserPrincipal principal) {
        return ResponseEntity.ok(authService.me(principal));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update own profile")
    public ResponseEntity<AuthUserResponse> updateProfile(
        @AuthenticationPrincipal AegisUserPrincipal principal,
        @Valid @RequestBody UpdateProfileRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.updateProfile(principal, request, httpRequest));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<MessageResponse> changePassword(
        @AuthenticationPrincipal AegisUserPrincipal principal,
        @Valid @RequestBody ChangePasswordRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.changePassword(principal, request, httpRequest));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Always returns a generic success message.")
    public ResponseEntity<MessageResponse> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.forgotPassword(request, httpRequest));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password")
    public ResponseEntity<MessageResponse> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.resetPassword(request, httpRequest));
    }
}
