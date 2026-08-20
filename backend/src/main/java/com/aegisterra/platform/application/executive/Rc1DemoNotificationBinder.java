package com.aegisterra.platform.application.executive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Binds RC1 demo notifications after IAM seed runs.
 * Liquibase inserts admin notifications with null user_id because admin is created at runtime.
 * Persona inboxes are inserted here for the same reason (demo role users are bootstrapped at runtime).
 */
@Component
public class Rc1DemoNotificationBinder {

    private static final Logger log = LoggerFactory.getLogger(Rc1DemoNotificationBinder.class);

    private final JdbcTemplate jdbc;

    public Rc1DemoNotificationBinder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bindDemoNotifications() {
        int updated = jdbc.update(
            """
            UPDATE notifications n
            SET user_id = u.id, updated_at = CURRENT_TIMESTAMP
            FROM users u
            WHERE u.username = 'admin' AND u.deleted = false
              AND n.id IN (
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeeec01'::uuid,
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeeec02'::uuid,
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeeec03'::uuid
              )
              AND n.user_id IS NULL
            """
        );
        if (updated > 0) {
            log.info("Bound {} RC1 demo notifications to admin user", updated);
        }

        int personaInserted = jdbc.update(
            """
            INSERT INTO notifications (
              id, user_id, channel, title, body, sent_at, read_at,
              created_at, updated_at, version, status, deleted
            )
            SELECT v.id::uuid, u.id, 'IN_APP', v.title, v.body, CURRENT_TIMESTAMP, NULL,
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'SENT', FALSE
            FROM (VALUES
              (
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeeed01',
                'farmer.demo',
                'Drought alert for your farm',
                'Climate Intelligence opened alert ALT-2026-001 for farm FARM-2026-001.'
              ),
              (
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeeed02',
                'insurance.officer',
                'Claim submitted for validation',
                'Claim CLM-2026-001 was submitted and awaits inspection and assessment.'
              ),
              (
                'eeeeeeee-eeee-eeee-eeee-eeeeeeeeed03',
                'fi.officer',
                'Settlement pending review',
                'Settlement SET-2026-002 requires finance review before payment execution.'
              )
            ) AS v(id, username, title, body)
            JOIN users u ON u.username = v.username AND u.deleted = false
            WHERE NOT EXISTS (SELECT 1 FROM notifications n WHERE n.id = v.id::uuid)
            """
        );
        if (personaInserted > 0) {
            log.info("Bound {} demo notifications to farmer / insurance / FI personas", personaInserted);
        }
    }
}
