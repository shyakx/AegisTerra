package com.aegisterra.platform.application.identity;

import com.aegisterra.platform.config.SecurityProperties;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.identity.PasswordPolicy;
import com.aegisterra.platform.domain.identity.SessionStatus;
import com.aegisterra.platform.domain.identity.TokenStatus;
import com.aegisterra.platform.domain.identity.UserStatus;
import com.aegisterra.platform.infrastructure.persistence.identity.LoginSessionEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.LoginSessionRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.PasswordHistoryEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.PasswordHistoryRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.PasswordResetTokenEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.PasswordResetTokenRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.PermissionEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.PermissionRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.RefreshTokenEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.RefreshTokenRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleRepository;
import com.aegisterra.platform.infrastructure.persistence.agriculture.FarmerRepository;
import com.aegisterra.platform.infrastructure.persistence.identity.UserEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRepository;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.infrastructure.security.AuthCookieService;
import com.aegisterra.platform.infrastructure.security.JwtService;
import com.aegisterra.platform.infrastructure.security.LoginRateLimiter;
import com.aegisterra.platform.infrastructure.security.TokenHasher;
import com.aegisterra.platform.application.contracts.AuthUserResponse;
import com.aegisterra.platform.application.contracts.ChangePasswordRequest;
import com.aegisterra.platform.application.contracts.ForgotPasswordRequest;
import com.aegisterra.platform.application.contracts.LoginRequest;
import com.aegisterra.platform.application.contracts.LoginResponse;
import com.aegisterra.platform.application.contracts.MessageResponse;
import com.aegisterra.platform.application.contracts.ResetPasswordRequest;
import com.aegisterra.platform.application.contracts.UpdateProfileRequest;
import com.aegisterra.platform.shared.exception.AccountLockedException;
import com.aegisterra.platform.shared.exception.RateLimitException;
import com.aegisterra.platform.shared.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int PASSWORD_HISTORY_SIZE = 5;
    private static final long RESET_TOKEN_MINUTES = 60;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthCookieService authCookieService;
    private final LoginRateLimiter loginRateLimiter;
    private final SecurityProperties securityProperties;
    private final AuditService auditService;
    private final FarmerRepository farmerRepository;

    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PermissionRepository permissionRepository,
        LoginSessionRepository loginSessionRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordHistoryRepository passwordHistoryRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthCookieService authCookieService,
        LoginRateLimiter loginRateLimiter,
        SecurityProperties securityProperties,
        AuditService auditService,
        FarmerRepository farmerRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.loginSessionRepository = loginSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authCookieService = authCookieService;
        this.loginRateLimiter = loginRateLimiter;
        this.securityProperties = securityProperties;
        this.auditService = auditService;
        this.farmerRepository = farmerRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String ip = clientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        String rateKey = ip + "|" + request.username().toLowerCase();
        if (!loginRateLimiter.tryAcquire(rateKey) || !loginRateLimiter.tryAcquire("ip:" + ip)) {
            auditService.record(AuditAction.RATE_LIMIT_EXCEEDED, null, "auth", request.username(), ip, ua, null);
            throw new RateLimitException("Too many login attempts. Try again later.");
        }

        Optional<UserEntity> userOpt = userRepository.findByUsernameIgnoreCaseAndDeletedFalse(request.username());
        if (userOpt.isEmpty()) {
            auditService.record(AuditAction.LOGIN_FAILURE, null, "auth", request.username(), ip, ua, "{\"reason\":\"unknown_user\"}");
            throw new UnauthorizedException("Invalid username or password");
        }

        UserEntity user = userOpt.get();
        unlockIfExpired(user);

        if (UserStatus.LOCKED.name().equals(user.getStatus())
            || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now()))) {
            auditService.record(AuditAction.LOGIN_FAILURE, user.getId(), "user", user.getId().toString(), ip, ua, "{\"reason\":\"locked\"}");
            throw new AccountLockedException("Account is locked. Try again later or contact an administrator.");
        }

        if (!UserStatus.ACTIVE.name().equals(user.getStatus())) {
            auditService.record(AuditAction.LOGIN_FAILURE, user.getId(), "user", user.getId().toString(), ip, ua, "{\"reason\":\"inactive\"}");
            throw new UnauthorizedException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user, ip, ua);
            throw new UnauthorizedException("Invalid username or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
        issueSessionCookies(user, rememberMe, ip, ua, httpResponse);
        auditService.record(AuditAction.LOGIN_SUCCESS, user.getId(), "user", user.getId().toString(), ip, ua,
            rememberMe ? "{\"rememberMe\":true}" : null);
        return new LoginResponse(toAuthUser(user));
    }

    @Transactional
    public LoginResponse refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String rawRefresh = authCookieService.readRefreshToken(httpRequest)
            .orElseThrow(() -> new UnauthorizedException("Refresh token missing"));
        String ip = clientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");

        String hash = TokenHasher.sha256(rawRefresh);
        RefreshTokenEntity token = refreshTokenRepository.findByTokenHashAndDeletedFalse(hash)
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (TokenStatus.REVOKED.name().equals(token.getStatus())
            || TokenStatus.ROTATED.name().equals(token.getStatus())) {
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            loginSessionRepository.revokeFamily(token.getFamilyId());
            authCookieService.clearAuthCookies(httpResponse);
            auditService.record(AuditAction.TOKEN_REUSE_DETECTED, token.getUserId(), "refresh_token",
                token.getId().toString(), ip, ua, null);
            throw new UnauthorizedException("Refresh token reuse detected");
        }

        if (!TokenStatus.ACTIVE.name().equals(token.getStatus()) || token.getExpiresAt().isBefore(Instant.now())) {
            token.setStatus(TokenStatus.EXPIRED.name());
            refreshTokenRepository.save(token);
            authCookieService.clearAuthCookies(httpResponse);
            throw new UnauthorizedException("Refresh token expired");
        }

        UserEntity user = userRepository.findByIdAndDeletedFalse(token.getUserId())
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!UserStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new UnauthorizedException("User not active");
        }

        LoginSessionEntity session = loginSessionRepository.findByIdAndDeletedFalse(token.getSessionId())
            .orElseThrow(() -> new UnauthorizedException("Session not found"));
        if (!SessionStatus.ACTIVE.name().equals(session.getStatus()) || session.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Session expired");
        }

        token.setStatus(TokenStatus.ROTATED.name());
        refreshTokenRepository.save(token);

        long refreshDays = session.isRememberMe()
            ? securityProperties.getJwt().getRememberMeDays()
            : securityProperties.getJwt().getRefreshTokenDays();
        Instant refreshExpiry = Instant.now().plus(refreshDays, ChronoUnit.DAYS);

        String newRaw = TokenHasher.generateOpaqueToken();
        RefreshTokenEntity newToken = new RefreshTokenEntity();
        newToken.setUserId(user.getId());
        newToken.setSessionId(session.getId());
        newToken.setFamilyId(token.getFamilyId());
        newToken.setTokenHash(TokenHasher.sha256(newRaw));
        newToken.setExpiresAt(refreshExpiry);
        newToken.setRotatedFromId(token.getId());
        newToken.setStatus(TokenStatus.ACTIVE.name());
        refreshTokenRepository.save(newToken);

        session.setLastSeenAt(Instant.now());
        loginSessionRepository.save(session);

        writeCookies(user, session, newRaw, refreshExpiry, httpResponse);
        auditService.record(AuditAction.TOKEN_REFRESH, user.getId(), "session", session.getId().toString(), ip, ua, null);
        return new LoginResponse(toAuthUser(user));
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse, AegisUserPrincipal principal) {
        String ip = clientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        authCookieService.readRefreshToken(httpRequest).ifPresent(rawRefresh ->
            refreshTokenRepository.findByTokenHashAndDeletedFalse(TokenHasher.sha256(rawRefresh)).ifPresent(token -> {
                refreshTokenRepository.revokeFamily(token.getFamilyId());
                loginSessionRepository.revokeFamily(token.getFamilyId());
            })
        );
        authCookieService.clearAuthCookies(httpResponse);
        UUID actor = principal != null ? principal.id() : null;
        auditService.record(AuditAction.LOGOUT, actor, "auth", actor != null ? actor.toString() : null, ip, ua, null);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(AegisUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        UserEntity user = userRepository.findByIdAndDeletedFalse(principal.id())
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        return toAuthUser(user);
    }

    @Transactional
    public AuthUserResponse updateProfile(
        AegisUserPrincipal principal,
        UpdateProfileRequest request,
        HttpServletRequest httpRequest
    ) {
        if (principal == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        UserEntity user = userRepository.findByIdAndDeletedFalse(principal.id())
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (request.email() != null && !request.email().isBlank()
            && !request.email().equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
                });
            user.setEmail(request.email().trim());
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName().isBlank() ? null : request.displayName().trim());
        }
        user.setUpdatedBy(principal.id());
        userRepository.save(user);
        auditService.record(
            AuditAction.USER_UPDATED,
            principal.id(),
            "user",
            user.getId().toString(),
            clientIp(httpRequest),
            httpRequest.getHeader("User-Agent"),
            "{\"scope\":\"self_profile\"}"
        );
        return toAuthUser(user);
    }

    @Transactional
    public MessageResponse changePassword(
        AegisUserPrincipal principal,
        ChangePasswordRequest request,
        HttpServletRequest httpRequest
    ) {
        if (principal == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        UserEntity user = userRepository.findByIdAndDeletedFalse(principal.id())
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        requireValidPassword(request.newPassword(), user.getUsername(), user.getEmail());
        ensureNotInHistory(user, request.newPassword());
        applyPasswordChange(user, request.newPassword());
        refreshTokenRepository.revokeAllActiveForUser(user.getId());
        loginSessionRepository.revokeAllActiveForUser(user.getId());
        auditService.record(AuditAction.PASSWORD_CHANGE, user.getId(), "user", user.getId().toString(),
            clientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
        return new MessageResponse("Password changed successfully");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        if (!loginRateLimiter.tryAcquire("forgot:" + ip)) {
            throw new RateLimitException("Too many requests. Try again later.");
        }

        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email());
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            String raw = TokenHasher.generateOpaqueToken();
            PasswordResetTokenEntity reset = new PasswordResetTokenEntity();
            reset.setUserId(user.getId());
            reset.setTokenHash(TokenHasher.sha256(raw));
            reset.setExpiresAt(Instant.now().plus(RESET_TOKEN_MINUTES, ChronoUnit.MINUTES));
            reset.setStatus(TokenStatus.ACTIVE.name());
            passwordResetTokenRepository.save(reset);
            auditService.record(AuditAction.PASSWORD_RESET_REQUEST, user.getId(), "user", user.getId().toString(), ip, ua, null);
            log.info("Password reset token issued for user {} (local stub — not emailed): {}", user.getUsername(), raw);
        } else {
            auditService.record(AuditAction.PASSWORD_RESET_REQUEST, null, "auth", request.email(), ip, ua, "{\"found\":false}");
        }
        return new MessageResponse("If an account exists for that email, a reset link has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String hash = TokenHasher.sha256(request.token());
        PasswordResetTokenEntity reset = passwordResetTokenRepository.findByTokenHashAndDeletedFalse(hash)
            .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));
        if (!TokenStatus.ACTIVE.name().equals(reset.getStatus())
            || reset.getConsumedAt() != null
            || reset.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Invalid or expired reset token");
        }

        UserEntity user = userRepository.findByIdAndDeletedFalse(reset.getUserId())
            .orElseThrow(() -> new UnauthorizedException("User not found"));
        requireValidPassword(request.newPassword(), user.getUsername(), user.getEmail());
        ensureNotInHistory(user, request.newPassword());
        applyPasswordChange(user, request.newPassword());

        reset.setConsumedAt(Instant.now());
        reset.setStatus(TokenStatus.CONSUMED.name());
        passwordResetTokenRepository.save(reset);

        refreshTokenRepository.revokeAllActiveForUser(user.getId());
        loginSessionRepository.revokeAllActiveForUser(user.getId());
        auditService.record(AuditAction.PASSWORD_RESET_SUCCESS, user.getId(), "user", user.getId().toString(),
            clientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
        return new MessageResponse("Password has been reset");
    }

    private void issueSessionCookies(
        UserEntity user,
        boolean rememberMe,
        String ip,
        String ua,
        HttpServletResponse response
    ) {
        UUID familyId = UUID.randomUUID();
        long refreshDays = rememberMe
            ? securityProperties.getJwt().getRememberMeDays()
            : securityProperties.getJwt().getRefreshTokenDays();
        Instant refreshExpiry = Instant.now().plus(refreshDays, ChronoUnit.DAYS);

        LoginSessionEntity session = new LoginSessionEntity();
        session.setUserId(user.getId());
        session.setFamilyId(familyId);
        session.setIpAddress(ip);
        session.setUserAgent(ua != null && ua.length() > 512 ? ua.substring(0, 512) : ua);
        session.setRememberMe(rememberMe);
        session.setLastSeenAt(Instant.now());
        session.setExpiresAt(refreshExpiry);
        session.setStatus(SessionStatus.ACTIVE.name());
        loginSessionRepository.save(session);

        String rawRefresh = TokenHasher.generateOpaqueToken();
        RefreshTokenEntity refresh = new RefreshTokenEntity();
        refresh.setUserId(user.getId());
        refresh.setSessionId(session.getId());
        refresh.setFamilyId(familyId);
        refresh.setTokenHash(TokenHasher.sha256(rawRefresh));
        refresh.setExpiresAt(refreshExpiry);
        refresh.setStatus(TokenStatus.ACTIVE.name());
        refreshTokenRepository.save(refresh);

        writeCookies(user, session, rawRefresh, refreshExpiry, response);
    }

    private void writeCookies(
        UserEntity user,
        LoginSessionEntity session,
        String rawRefresh,
        Instant refreshExpiry,
        HttpServletResponse response
    ) {
        List<String> roles = roleRepository.findActiveRolesByUserId(user.getId()).stream()
            .map(RoleEntity::getCode)
            .toList();
        List<String> permissions = permissionRepository.findActivePermissionsByUserId(user.getId()).stream()
            .map(PermissionEntity::getCode)
            .toList();
        String access = jwtService.createAccessToken(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            roles,
            permissions,
            user.getTokenVersion(),
            session.getId()
        );
        authCookieService.writeAccessToken(response, access, jwtService.accessTokenMaxAgeSeconds());
        long refreshMaxAge = Math.max(1, refreshExpiry.getEpochSecond() - Instant.now().getEpochSecond());
        authCookieService.writeRefreshToken(response, rawRefresh, refreshMaxAge);
    }

    private void handleFailedLogin(UserEntity user, String ip, String ua) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= securityProperties.getLockout().getMaxFailedAttempts()) {
            user.setStatus(UserStatus.LOCKED.name());
            user.setLockedUntil(Instant.now().plus(securityProperties.getLockout().getLockMinutes(), ChronoUnit.MINUTES));
            auditService.record(AuditAction.ACCOUNT_LOCKED, user.getId(), "user", user.getId().toString(), ip, ua, null);
        }
        userRepository.save(user);
        auditService.record(AuditAction.LOGIN_FAILURE, user.getId(), "user", user.getId().toString(), ip, ua,
            "{\"attempts\":" + attempts + "}");
    }

    private void unlockIfExpired(UserEntity user) {
        if (UserStatus.LOCKED.name().equals(user.getStatus())
            && user.getLockedUntil() != null
            && user.getLockedUntil().isBefore(Instant.now())) {
            user.setStatus(UserStatus.ACTIVE.name());
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            auditService.record(AuditAction.ACCOUNT_UNLOCKED, user.getId(), "user", user.getId().toString(),
                null, null, "{\"reason\":\"lock_expired\"}");
        }
    }

    private void requireValidPassword(String password, String username, String email) {
        List<String> errors = PasswordPolicy.validate(password, username, email);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    private void ensureNotInHistory(UserEntity user, String newPassword) {
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must differ from the current password");
        }
        List<PasswordHistoryEntity> history = passwordHistoryRepository
            .findTop5ByUserIdAndDeletedFalseOrderByCreatedAtDesc(user.getId());
        int checked = 0;
        for (PasswordHistoryEntity entry : history) {
            if (checked >= PASSWORD_HISTORY_SIZE) {
                break;
            }
            if (passwordEncoder.matches(newPassword, entry.getPasswordHash())) {
                throw new IllegalArgumentException("Password was used recently and cannot be reused");
            }
            checked++;
        }
    }

    private void applyPasswordChange(UserEntity user, String newPassword) {
        PasswordHistoryEntity history = new PasswordHistoryEntity();
        history.setUserId(user.getId());
        history.setPasswordHash(user.getPasswordHash());
        history.setStatus("ACTIVE");
        passwordHistoryRepository.save(history);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    public AuthUserResponse toAuthUser(UserEntity user) {
        List<String> roles = roleRepository.findActiveRolesByUserId(user.getId()).stream()
            .map(RoleEntity::getCode)
            .toList();
        List<String> permissions = permissionRepository.findActivePermissionsByUserId(user.getId()).stream()
            .map(PermissionEntity::getCode)
            .toList();
        UUID farmerId = farmerRepository.findByUserIdAndDeletedFalse(user.getId())
            .map(farmer -> farmer.getId())
            .orElse(null);
        return new AuthUserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            user.getStatus(),
            user.isMustChangePassword(),
            roles,
            permissions,
            farmerId
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
