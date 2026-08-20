package com.aegisterra.platform.infrastructure.security;

import com.aegisterra.platform.infrastructure.persistence.identity.UserEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthCookieService authCookieService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
        AuthCookieService authCookieService,
        JwtService jwtService,
        UserRepository userRepository
    ) {
        this.authCookieService = authCookieService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authCookieService.readAccessToken(request).ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtService.parse(token);
            UUID userId = UUID.fromString(claims.getSubject());
            Integer tokenVersion = claims.get("tv", Integer.class);
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null || user.isDeleted() || !"ACTIVE".equals(user.getStatus())) {
                return;
            }
            if (tokenVersion == null || user.getTokenVersion() != tokenVersion) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);

            Collection<SimpleGrantedAuthority> authorities = Stream.concat(
                    roles == null ? Stream.of() : roles.stream(),
                    permissions == null ? Stream.of() : permissions.stream()
                )
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

            AegisUserPrincipal principal = new AegisUserPrincipal(
                userId,
                user.getUsername(),
                user.getEmail(),
                roles == null ? List.of() : List.copyOf(roles),
                permissions == null ? List.of() : List.copyOf(permissions)
            );
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities)
            );
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }
    }
}
