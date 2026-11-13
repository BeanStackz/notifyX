# NotifyX Outbox and RabbitMQ Context

## Scope

The notification ingestion flow now persists a notification and its
`NotificationAcceptedEvent` outbox record in the same database transaction.
A scheduled publisher later claims pending outbox records and publishes them
to RabbitMQ.

This provides reliable, at-least-once publication without holding a database
transaction open during broker communication.

## Notification request flow

1. A client sends `POST /api/v1/ingest/notifications`.
2. `ApiKeyAuthenticationFilter` authenticates the project API key.
3. The notification service validates the project, organization, request, and
   future scheduling timestamp.
4. The service checks the project and idempotency key before reserving quota.
5. The active plan validates the channel and supplies the notification
   priority.
6. The notification is saved with `ACCEPTED` status.
7. An `OutboxEvent` is saved with `PENDING` status.
8. The API returns `202 Accepted`.

The notification and outbox insert use one transaction. If either operation
fails, neither record should be committed.

## Outbox lifecycle

`OutboxEvent` uses these statuses:

- `PENDING`: ready for publication or waiting for retry.
- `PROCESSING`: claimed by a publisher instance.
- `PUBLISHED`: RabbitMQ confirmed the message.
- `FAILED`: maximum publication attempts were exhausted.

The entity also stores:

- `attempts`: number of failed publication attempts.
- `nextAttemptAt`: earliest time for the next retry.
- `lockedAt`: time the event was claimed.
- `lockedBy`: publisher instance identifier.
- `lastError`: latest publication failure.
- `eventVersion`: event schema version, currently `1`.
- `publishedAt`: successful publication timestamp.

## Claiming and publishing

`OutboxPersistenceService` claims records in a short transaction using
PostgreSQL `FOR UPDATE SKIP LOCKED`.

Only events that are either pending and due or stale processing records are
selected. A stale processing record is one whose lock is older than the
configured lock timeout.

The claim transaction:

1. Selects a batch.
2. Sets each event to `PROCESSING`.
3. Stores `lockedAt` and `lockedBy`.
4. Commits.

`OutboxPublisherService` then publishes outside the database transaction.
After RabbitMQ confirms the message, the event is marked `PUBLISHED` in a new
transaction. If publishing fails, the event is returned to `PENDING` with
exponential backoff, or marked `FAILED` after the maximum attempts.

## RabbitMQ topology

The topology is declared by `RabbitTopologyConfig`:

| Resource | Name |
|---|---|
| Main exchange | `notifyx.notification.exchange` |
| Main queue | `notifyx.notification.queue` |
| Main routing key | `notification` |
| Dead-letter exchange | `notifyx.notification.dlx` |
| Dead-letter queue | `notifyx.notification.dlq` |
| Dead-letter routing key | `notification.dead` |

The main queue is durable and has `x-max-priority=4`. Messages are assigned
priority from the event payload:

- `CRITICAL` = 4
- `HIGH` = 3
- `NORMAL` = 2
- `LOW` = 1

The queue is configured with the dead-letter exchange and routing key.

## Configuration

The local profile contains:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true

notifyx.outbox.batch-size=50
notifyx.outbox.poll-interval-ms=1000
notifyx.outbox.max-attempts=10
notifyx.outbox.initial-retry-delay-seconds=2
notifyx.outbox.max-retry-delay-seconds=300
notifyx.outbox.lock-timeout-seconds=120
```

RabbitMQ must be running before the application starts if the publisher and
topology are enabled.

## Delivery guarantee

The current implementation provides **at-least-once publication**.

An application crash can happen after RabbitMQ accepts a message but before
the database event is marked `PUBLISHED`. The stale lock will eventually be
reclaimed and the event may be published again.

Consumers must therefore:

- Treat the outbox event ID/message ID as an idempotency key.
- Safely ignore an event that was already processed.
- Use manual acknowledgements.

Exactly-once delivery is not provided by this design.

## Current limitations

The following components are not implemented yet:

- RabbitMQ notification consumer.
- Email, SMS, or push provider integration.
- Delivery-attempt persistence.
- Consumer retry and permanent-failure handling.
- Scheduled notification dispatcher.
- Delivery metrics, structured delivery logs, and custom health indicators.

## Brevo email delivery

Email delivery uses Brevo's transactional email API through
`EmailProvider`. The provider is active when:

```properties
notifyx.email.provider=brevo
```

Required environment variables:

```text
BREVO_API_KEY=your_brevo_api_key
BREVO_SENDER_EMAIL=verified-sender@example.com
BREVO_SENDER_NAME=NotifyX
```

The sender address must be verified in Brevo. The API key is sent only in the
backend `api-key` header and is never returned to the frontend.

The provider calls:

```text
POST https://api.brevo.com/v3/smtp/email
```

The current provider sends the notification content as escaped HTML and uses
`NotifyX notification` when no subject is supplied.

## Consumer behavior

`NotificationEventConsumer` uses manual acknowledgements. A successful Brevo
delivery marks the notification `SENT` and acknowledges the RabbitMQ message.
Provider or validation failures mark the delivery attempt and notification as
failed, then reject the message without requeueing so RabbitMQ routes it to
the configured dead-letter queue.

Consumer retry queues are still pending. Durable event-receipt idempotency is
implemented with a unique event ID and stale-claim recovery.
Until those are implemented, the DLQ is the terminal path for consumer
failures, while the outbox publisher itself continues to retry publication
failures.

Brevo webhooks are received at `POST /api/v1/webhooks/brevo`. Configure the
same `BREVO_WEBHOOK_SECRET` value in Brevo's custom
`X-Brevo-Webhook-Key` header. Events are stored idempotently in
`delivery_events` and correlated using Brevo's `message-id`.

The authenticated frontend tracking endpoint is:

`GET /api/v1/projects/{projectId}/notifications/{notificationId}`

It returns notification status, delivery attempts, provider message IDs,
provider responses/errors, and received delivery/open/click/bounce events.
The caller must be a member of the project's organization.

The `scheduledAt` value is currently stored in the notification and outbox
payload. The current publisher does not delay publication based on that value;
a scheduling dispatcher must be added before delayed delivery is supported.

## Compatibility notes

- PostgreSQL is required for the native `SKIP LOCKED` claim query.
- The database schema uses `ddl-auto=update` locally, but production should
  use an explicit migration for the new outbox columns and indexes.
- RabbitMQ must support publisher confirms and durable queues.
- The publisher uses Spring AMQP from `spring-boot-starter-amqp`.
- Existing notification ingestion remains API-key based and continues to
  return `202 Accepted`.
