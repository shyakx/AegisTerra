package com.aegisterra.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aegisterra.security")
public class SecurityProperties {

    private final Jwt jwt = new Jwt();
    private final Cookie cookie = new Cookie();
    private final Lockout lockout = new Lockout();
    private final Cors cors = new Cors();
    private final Bootstrap bootstrap = new Bootstrap();

    public Jwt getJwt() {
        return jwt;
    }

    public Cookie getCookie() {
        return cookie;
    }

    public Lockout getLockout() {
        return lockout;
    }

    public Cors getCors() {
        return cors;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public static class Jwt {
        private String secret = "local-only-change-me-aegisterra-jwt-secret-key-32bytes-min";
        private String issuer = "aegisterra";
        private long accessTokenMinutes = 15;
        private long refreshTokenDays = 7;
        private long rememberMeDays = 30;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public long getAccessTokenMinutes() {
            return accessTokenMinutes;
        }

        public void setAccessTokenMinutes(long accessTokenMinutes) {
            this.accessTokenMinutes = accessTokenMinutes;
        }

        public long getRefreshTokenDays() {
            return refreshTokenDays;
        }

        public void setRefreshTokenDays(long refreshTokenDays) {
            this.refreshTokenDays = refreshTokenDays;
        }

        public long getRememberMeDays() {
            return rememberMeDays;
        }

        public void setRememberMeDays(long rememberMeDays) {
            this.rememberMeDays = rememberMeDays;
        }
    }

    public static class Cookie {
        private String accessName = "at";
        private String refreshName = "rt";
        private boolean secure = false;
        private String sameSite = "Lax";

        public String getAccessName() {
            return accessName;
        }

        public void setAccessName(String accessName) {
            this.accessName = accessName;
        }

        public String getRefreshName() {
            return refreshName;
        }

        public void setRefreshName(String refreshName) {
            this.refreshName = refreshName;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String getSameSite() {
            return sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }
    }

    public static class Lockout {
        private int maxFailedAttempts = 5;
        private long lockMinutes = 30;
        private int loginRateLimitPerMinute = 20;

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public long getLockMinutes() {
            return lockMinutes;
        }

        public void setLockMinutes(long lockMinutes) {
            this.lockMinutes = lockMinutes;
        }

        public int getLoginRateLimitPerMinute() {
            return loginRateLimitPerMinute;
        }

        public void setLoginRateLimitPerMinute(int loginRateLimitPerMinute) {
            this.loginRateLimitPerMinute = loginRateLimitPerMinute;
        }
    }

    public static class Cors {
        private java.util.List<String> allowedOrigins = java.util.List.of("http://localhost:3000", "http://127.0.0.1:3000");

        public java.util.List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(java.util.List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Bootstrap {
        private boolean enabled = false;
        private String adminUsername = "admin";
        private String adminEmail = "admin@aegisterra.local";
        private String adminPassword = "";
        private boolean demoUsersEnabled = false;
        private String demoUsersPassword = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }

        public String getAdminEmail() {
            return adminEmail;
        }

        public void setAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }

        public boolean isDemoUsersEnabled() {
            return demoUsersEnabled;
        }

        public void setDemoUsersEnabled(boolean demoUsersEnabled) {
            this.demoUsersEnabled = demoUsersEnabled;
        }

        public String getDemoUsersPassword() {
            return demoUsersPassword;
        }

        public void setDemoUsersPassword(String demoUsersPassword) {
            this.demoUsersPassword = demoUsersPassword;
        }
    }
}
