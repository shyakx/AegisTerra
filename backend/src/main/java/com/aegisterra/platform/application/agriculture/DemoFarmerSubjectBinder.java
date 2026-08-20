package com.aegisterra.platform.application.agriculture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Links farmer portal user {@code farmer.demo} to seeded farmer {@code FRM-2026-001} after IAM bootstrap.
 */
@Component
public class DemoFarmerSubjectBinder {

    private static final Logger log = LoggerFactory.getLogger(DemoFarmerSubjectBinder.class);

    private final JdbcTemplate jdbc;

    public DemoFarmerSubjectBinder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bindDemoFarmer() {
        int updated = jdbc.update(
            """
            UPDATE farmers f
            SET user_id = u.id, updated_at = CURRENT_TIMESTAMP
            FROM users u
            WHERE u.username = 'farmer.demo' AND u.deleted = false
              AND f.farmer_code IN ('FRM-2026-001', 'FRM-RC1-001') AND f.deleted = false
              AND (f.user_id IS NULL OR f.user_id <> u.id)
            """
        );
        if (updated > 0) {
            log.info("Bound farmer FRM-2026-001 to user farmer.demo");
        }
    }
}
