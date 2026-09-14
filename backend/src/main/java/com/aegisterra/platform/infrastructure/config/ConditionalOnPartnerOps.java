package com.aegisterra.platform.infrastructure.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Partner insurance ops (claims, settlements, policies, workflow desks, lending).
 * Disabled by default per ADR-010 — enable with {@code aegisterra.modules.partner-ops=true}.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "aegisterra.modules", name = "partner-ops", havingValue = "true")
public @interface ConditionalOnPartnerOps {
}
