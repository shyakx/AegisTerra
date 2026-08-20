package com.aegisterra.platform.infrastructure.security;

import com.aegisterra.platform.config.SecurityProperties;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private final SecurityProperties securityProperties;
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        long windowSeconds = 60;
        int max = securityProperties.getLockout().getLoginRateLimitPerMinute();
        Deque<Instant> window = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(now.minusSeconds(windowSeconds))) {
                window.removeFirst();
            }
            if (window.size() >= max) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }
}
