# C4 — Component: Communication & Event Platform (Stage 6D)

```text
[WorkflowRuntime / TaskService / DecisionService]
    │  EventBus.publish(PlatformDomainEvent)
    ▼
[SpringApplicationEventBus]  ←── port swap → Kafka/RabbitMQ later
    │
    ▼
[WorkflowNotificationHandler]  (@TransactionalEventListener AFTER_COMMIT)
    │
    ▼
[NotificationService]
    ├── NotificationPreferenceService
    ├── TemplateService
    ├── ChannelResolver
    └── NotificationDispatcher
            ├── InAppProvider → notifications
            ├── EmailProvider (stub)
            ├── SmsProvider (stub)
            ├── PushProvider (stub)
            └── WebhookProvider (stub)

[NotificationController] ← operator inbox / preferences
```
