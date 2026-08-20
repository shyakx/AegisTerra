# AegisTerra Event & Communication Architecture

**Version:** 1.0.0 (Stage 6D)  
**Status:** Delivered with Stage 6D (**0.10.0**)  
**Related:** `WorkflowArchitecture.md`, ADR-008  

## 1. Purpose

Business modules **must never** send email, SMS, push, or in-app messages directly.

```text
Business Module
      ↓
Publish Domain Event  (EventBus port)
      ↓
Event Bus transport   (Spring in-process now; Kafka/RabbitMQ later)
      ↓
Communication Engine
      ↓
Channel Providers     (IN_APP implemented; EMAIL/SMS/PUSH/WEBHOOK stubs)
```

Swapping the transport (Kafka, RabbitMQ, Azure Service Bus) must **not** require changing Claims, Policy, or Workflow application services — only the `EventBus` adapter.

---

## 2. Event publishing

- Modules depend on `EventBus` (port), not on Spring or brokers.
- Stage 6D transport: `SpringApplicationEventBus` → `ApplicationEventPublisher`.
- Publish **after** durable state is committed where delivery side-effects matter (`@TransactionalEventListener(AFTER_COMMIT)` on the consumer side).
- Every event is a `DomainEvent` with: `eventId`, `eventType`, `occurredAt`, `actorId`, `subjectType`, `subjectId`, `correlationId`, `payload`.

### Canonical event types (6D)

| Type | Publisher |
|------|-----------|
| `WorkflowStarted` | WorkflowRuntime |
| `WorkflowCompleted` | WorkflowRuntime (terminal COMPLETED) |
| `WorkflowCancelled` | WorkflowRuntime (terminal CANCELLED/REJECTED) |
| `TaskCreated` | WorkflowTaskFactory |
| `TaskAssigned` | TaskAssignmentService |
| `TaskClaimed` | TaskService |
| `TaskCompleted` | TaskService |
| `DecisionRecorded` | DecisionService |
| `NotificationRequested` | NotificationDispatcher (before channel send) |
| `NotificationDelivered` | NotificationDispatcher (success) |
| `NotificationFailed` | NotificationDispatcher (provider failure) |

---

## 3. Event handling

- Handlers live in the Communication module (`WorkflowNotificationHandler`).
- Stage 6D uses synchronous Spring `@EventListener` in-process (same DB transaction as the publisher) for reliable IN_APP delivery.
- Future broker consumers should invoke `NotificationService` after commit / via outbox.
- Handlers are **idempotent-friendly**: delivery log records attempts; future outbox dedupes by `eventId`.
- Unknown event types are ignored (no hard failure).
- Business modules do not import channel providers.

---

## 4. Channel abstraction

| Provider interface | 6D implementation |
|--------------------|-------------------|
| `InAppProvider` | Persists to `notifications` |
| `EmailProvider` | Logging/stub |
| `SmsProvider` | Logging/stub |
| `PushProvider` | Logging/stub |
| `WebhookProvider` | Logging/stub |

`ChannelResolver` maps event + preferences → ordered channel list (default: `IN_APP`).

---

## 5. Delivery pipeline

```text
DomainEvent
  → resolve recipients (assignee / role pool / actor)
  → NotificationPreferenceService.filter(channels)
  → TemplateService.render(templateCode, vars)
  → publish NotificationRequested
  → NotificationDispatcher → provider.send
  → notification_delivery_log
  → publish NotificationDelivered | NotificationFailed
```

**Retry (6D):** synchronous single attempt + log failure. Exponential backoff / outbox worker is Stage later (documented dead-letter strategy).

**Dead-letter (future):** failed deliveries after N retries land in `notification_dead_letters` (not created in 6D); ops replay API.

---

## 6. Notification preferences

Per user: `(channel, eventType, enabled)`.  
Missing row ⇒ **enabled** for `IN_APP`; disabled for stub channels unless explicitly enabled.

---

## 7. Event subscriptions

6D uses code-wired listeners for workflow/task/decision events.  
Future: `notification_subscriptions` / rules table (event type → template → audience expression) without changing publishers.

---

## 8. Template rendering

Templates live in `notification_templates` (`code`, `channel`, `locale`, `title_template`, `body_template`).  
Placeholders: `{{key}}`. No hardcoded operator-facing copy in Java beyond fallback `"Notification"` if template missing (logged).

---

## 9. Future Kafka / RabbitMQ compatibility

| Concern | Design |
|---------|--------|
| Envelope | Same `DomainEvent` JSON schema |
| Port | `EventBus.publish` |
| Consumer | Replace `@EventListener` with broker listener that deserializes envelope → same handlers |
| Ordering | `correlationId` + `subjectId` partition keys |
| At-least-once | Idempotency key = `eventId` on delivery log |

---

## 10. Security

- Users only read/mark their own notifications.
- Preferences scoped to authenticated user.
- Permissions: `notifications:read`, `notifications:write` (mark read / update preferences).
