# NotifyX Backend API

NotifyX is a multi-tenant notification backend. Users manage organizations and
projects, then issue project-scoped API keys for notification ingestion.
Accepted notifications are persisted with an outbox event and handled
asynchronously through RabbitMQ and the configured delivery provider. The
backend also provides delivery tracking, usage analytics, subscriptions,
billing, and operational health information.

## Viewing the API reference

Run the backend, then open:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

The JSON or YAML document can be saved and imported into another OpenAPI viewer
or API client for standalone viewing. Swagger UI and the OpenAPI document are
publicly readable; API operations remain protected by their normal security
requirements.

## Authentication

- **JWT bearer token:** user, organization, project, API-key management,
  tracking, analytics, subscription, and administration endpoints require a
  valid user access token. In Swagger UI, use **Authorize** and enter the token
  without the `Bearer ` prefix.
- **Project API key:** `POST /api/v1/ingest/notifications` and
  `GET /api/v1/ingest/whoami` require `X-NotifyX-API-Key`. Create keys through
  the authenticated API-key management endpoints. A newly created key is
  returned once; subsequent key listings do not include the secret.
- **Provider webhooks:** Brevo uses `X-Brevo-Webhook-Key` (or a Bearer value
  supplied by the provider); Razorpay uses `X-Razorpay-Signature` and
  `x-razorpay-event-id`.
- **Public authentication operations:** registration, login, and token refresh
  do not require a bearer token. Logout requires an authenticated user.

Most successful API responses use the common envelope:

```json
{
  "success": true,
  "message": "Request completed successfully",
  "data": {},
  "timestamp": "2026-01-01T12:00:00Z"
}
```

Webhook endpoints return an empty response. Ingestion returns `202 Accepted`
when the notification has been accepted for asynchronous processing.

## Endpoint groups

| Group | Endpoints |
|---|---|
| Authentication | `POST /api/v1/auth/register`, `/login`, `/refresh`, `/logout` |
| Profile | `GET` and `PUT /api/v1/users/profile` |
| Organizations | `POST /api/v1/organizations`; `GET /api/v1/organizations` and `/{organizationId}` |
| Projects | `POST` and `GET /api/v1/organizations/{organizationId}/projects`; `GET` and `PUT /{projectId}`; `PATCH /{projectId}/archive` |
| API keys | `POST` and `GET /api/v1/organizations/{organizationId}/projects/{projectId}/api-keys`; `DELETE /{apiKeyId}` |
| Notification ingestion | `POST /api/v1/ingest/notifications`; `GET /api/v1/ingest/whoami` |
| Delivery tracking | `GET /api/v1/projects/{projectId}/notifications`; `GET /{notificationId}`; `POST /{notificationId}/retry` |
| Provider webhooks | `POST /api/v1/webhooks/brevo`; `POST /api/v1/webhooks/razorpay` |
| Usage analytics | `GET /api/v1/organizations/{organizationId}/analytics` |
| Plans and billing | `GET /api/v1/plans`; billing and subscription routes under `/api/v1/organizations/{organizationId}` |
| Administration and health | `GET /api/v1/admin/dashboard`; `GET /api/v1/health/operations` |

Swagger UI contains the full path parameters, request and response schemas,
authentication requirements, and per-operation details.

## Projects

A project belongs to an organization and provides an isolated namespace for
notifications and API keys. Projects have a name, a unique-within-organization
slug, an optional description (up to 500 characters), and an active or archived
status. Organization owners and administrators can create, update, and archive
projects; organization members can list and view them. Archived projects
cannot be updated or used as active notification destinations.

Project create and update request/response schemas in Swagger describe the
project name, slug, description, status, organization ID, and timestamps.

## Notification ingestion notes

Supply an `Idempotency-Key` header when creating a notification. It prevents
duplicate creation for the same project and key. `scheduledAt` is accepted and
stored, but the current outbox publisher does not defer publication until that
time. Notification delivery is asynchronous and at-least-once; use the tracking
endpoints to inspect status and delivery attempts.
