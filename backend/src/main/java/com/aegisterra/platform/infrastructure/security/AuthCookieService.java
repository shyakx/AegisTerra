package com.aegisterra.platform.infrastructure.security;

import com.aegisterra.platform.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {

    public static final String REFRESH_PATH = "/api/v1/auth";

    private final SecurityProperties securityProperties;

    public AuthCookieService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public void writeAccessToken(HttpServletResponse response, String token, long maxAgeSeconds) {
        SecurityProperties.Cookie cfg = securityProperties.getCookie();
        ResponseCookie cookie = ResponseCookie.from(cfg.getAccessName(), token)
            .httpOnly(true)
            .secure(cfg.isSecure())
            .path("/")
            .maxAge(maxAgeSeconds)
            .sameSite(cfg.getSameSite())
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void writeRefreshToken(HttpServletResponse response, String token, long maxAgeSeconds) {
        SecurityProperties.Cookie cfg = securityProperties.getCookie();
        ResponseCookie cookie = ResponseCookie.from(cfg.getRefreshName(), token)
            .httpOnly(true)
            .secure(cfg.isSecure())
            .path(REFRESH_PATH)
            .maxAge(maxAgeSeconds)
            .sameSite("Strict")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        SecurityProperties.Cookie cfg = securityProperties.getCookie();
        ResponseCookie access = ResponseCookie.from(cfg.getAccessName(), "")
            .httpOnly(true)
            .secure(cfg.isSecure())
            .path("/")
            .maxAge(0)
            .sameSite(cfg.getSameSite())
            .build();
        ResponseCookie refresh = ResponseCookie.from(cfg.getRefreshName(), "")
            .httpOnly(true)
            .secure(cfg.isSecure())
            .path(REFRESH_PATH)
            .maxAge(0)
            .sameSite("Strict")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, access.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refresh.toString());
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7).trim();
            return token.isEmpty() ? Optional.empty() : Optional.of(token);
        }
        return Optional.ofNullable(readCookie(request, securityProperties.getCookie().getAccessName()));
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(readCookie(request, securityProperties.getCookie().getRefreshName()));
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
