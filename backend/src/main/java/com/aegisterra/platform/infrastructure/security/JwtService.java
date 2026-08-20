package com.aegisterra.platform.infrastructure.security;

import com.aegisterra.platform.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private final SecurityProperties securityProperties;
    private final SecretKey secretKey;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        String secret = securityProperties.getJwt().getSecret();
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "aegisterra.security.jwt.secret must be set (AEGISTERRA_JWT_SECRET) and at least 256 bits"
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(
        UUID userId,
        String username,
        String email,
        Collection<String> roles,
        Collection<String> permissions,
        int tokenVersion,
        UUID sessionId
    ) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getJwt().getAccessTokenMinutes() * 60);
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .issuer(securityProperties.getJwt().getIssuer())
            .subject(userId.toString())
            .claim("username", username)
            .claim("email", email)
            .claim("roles", List.copyOf(roles))
            .claim("permissions", List.copyOf(permissions))
            .claim("tv", tokenVersion)
            .claim("sid", sessionId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(secretKey)
            .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(securityProperties.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid access token", ex);
        }
    }

    public long accessTokenMaxAgeSeconds() {
        return securityProperties.getJwt().getAccessTokenMinutes() * 60;
    }
}
