
# NotifyX – Distributed Notification & Communication Platform

NotifyX is a multi-tenant notification platform designed to help applications integrate and manage notifications through a centralized API.

The platform provides organization and project management, secure API key authentication, JWT-based identity management, notification ingestion, asynchronous delivery, priority-based message processing, retry handling, and delivery tracking.

It is built using **Spring Boot with a modular monolith architecture**, separating business capabilities into independent modules while maintaining a single deployable backend application.

The system is designed around asynchronous processing, reliable message delivery, tenant isolation, and plan-based resource management.

---

## 🚀 Key Features

- JWT-based authentication and role-based authorization
- Organization and project management
- Project-scoped API key authentication
- Multi-tenant notification management
- Notification ingestion through REST APIs
- Idempotent notification requests
- Plan-based notification usage and quotas
- Redis-based rate limiting
- RabbitMQ-based asynchronous notification processing
- Priority-based message queue processing
- Transactional Outbox pattern for reliable message publishing
- Email notifications using Brevo
- Email template rendering with notification metadata
- Delivery attempt tracking
- Exponential backoff retry mechanism with jitter
- Failed notification management
- Brevo webhook integration for delivery status updates
- Notification analytics and usage tracking
- Razorpay payment integration for plan upgrades
- Operational health monitoring through Spring Boot Actuator

---

## 🏗️ System Architecture (High Level)

NotifyX follows a **Modular Monolith Architecture**.

Each business capability is implemented as an independent module with its own controllers, services, entities, repositories, and supporting components.

The modules communicate internally through application services and event-driven workflows.

```text
                    Client Applications
                            |
                            |
                     REST API Layer
                            |
                  Authentication & RBAC
                            |
             Organization / Project Context
                            |
              API Key Validation & Authorization
                            |
                  Rate Limiting (Redis)
                            |
                  Notification Module
                            |
               Validation & Idempotency
                            |
               Plan & Quota Enforcement
                            |
                   PostgreSQL
                            |
                Transactional Outbox
                            |
                  Outbox Publisher
                            |
                     RabbitMQ
                            |
              Priority-based Processing
                            |
                 Delivery Consumers
                            |
                  Provider Abstraction
                            |
                  +---------+---------+
                  |                   |
                Brevo             Future Providers
                Email             SMS / Push
                  |
             Delivery Result
                  |
            Delivery Tracking
                  |
          Analytics & Webhooks
```

### Architectural Responsibilities

- **Identity Module** handles user authentication and identity management.
- **Authorization Module** manages roles and access permissions.
- **Organization Module** manages tenants and organization membership.
- **Project Module** manages projects belonging to organizations.
- **API Key Module** provides project-level API authentication.
- **Notification Module** handles notification ingestion and lifecycle management.
- **Rate Limit Module** enforces request limits using Redis.
- **Outbox Module** ensures reliable event publishing.
- **Messaging Module** manages asynchronous message processing.
- **Delivery Module** handles notification providers, delivery attempts, and retries.
- **Subscription & Payment Modules** manage plan entitlements and payment workflows.
- **Analytics Module** provides notification usage and delivery statistics.

---

## 🔐 Authentication & Authorization

NotifyX implements token-based authentication using **JSON Web Tokens (JWT)** with Spring Security.

The platform separates user identity from project-level API authentication.

### Authentication Mechanisms

**1. User Authentication**

Users authenticate using JWT-based credentials.

JWT authentication is used for protected platform operations such as organization management, project management, and account-related functionality.

**2. Project API Key Authentication**

Applications integrate with NotifyX using project-scoped API keys.

Each API key is associated with a project and is used to authorize notification ingestion requests.

API keys are stored in hashed form rather than being persisted as plaintext credentials.

### Authorization

The authorization module provides role-based access control.

Permissions are evaluated in the context of the authenticated user and the organization or project they are accessing.

### Security Responsibilities

- JWT validation
- Authentication context management
- Role-based authorization
- Organization and project access control
- API key validation
- Hashed API key storage

---

## 🏢 Organization & Project Management

NotifyX follows a multi-tenant architecture.

An organization represents a tenant using the platform, while projects represent individual applications or systems that generate notifications.

### Organization Responsibilities

- Organization creation and management
- Organization membership
- Plan association
- Organization-level resource ownership

### Project Responsibilities

- Project creation and management
- Project-level API credentials
- Notification isolation
- Project-level usage tracking

### Tenant Hierarchy

```text
User
 |
 +---- Organization A
 |        |
 |        +---- Project 1
 |        |
 |        +---- Project 2
 |
 +---- Organization B
          |
          +---- Project 3
```

This structure allows multiple applications to use the same NotifyX organization while maintaining project-level separation.

---

## 📩 Notification Management

The notification module acts as the entry point for notification requests.

Applications submit notification requests through the NotifyX API using their project API keys.

### Notification Processing Responsibilities

- Validate incoming notification requests
- Identify the associated project
- Validate the requested channel
- Check notification idempotency
- Enforce plan-based quotas
- Apply rate limits
- Determine notification priority
- Persist notification records
- Publish notification events through the asynchronous processing pipeline

### Supported Notification Channels

The notification domain currently defines the following channels:

| Channel | Current Status |
|---------|----------------|
| EMAIL | Implemented |
| SMS | Planned provider integration |
| PUSH | Planned |
| IN_APP | Planned |

### Notification Lifecycle

A notification can move through the following states:

| Status | Description |
|--------|-------------|
| ACCEPTED | Notification request accepted |
| QUEUED | Notification waiting for processing |
| PROCESSING | Notification being processed |
| RETRYING | Notification scheduled for another attempt |
| SENT | Provider accepted the notification |
| FAILED | Notification delivery failed |
| CANCELLED | Notification cancelled |

The platform maintains notification records and delivery attempts to support tracking and failure analysis.

---

## ⚡ Rate Limiting & Usage Management

NotifyX uses Redis as the distributed storage layer for rate-limiting operations.

Rate limiting is integrated into the request processing pipeline to control notification ingestion.

### Responsibilities

- Protect the platform from excessive API requests
- Enforce project-level request limits
- Support plan-based usage policies
- Maintain distributed rate-limit state
- Prevent one tenant from consuming disproportionate resources

Rate limiting is designed to work alongside plan entitlements and notification quotas.

The rate-limit module contains dedicated algorithm, interceptor, configuration, model, and service packages.

---

## 📬 Asynchronous Messaging & Priority Processing

NotifyX uses RabbitMQ for asynchronous notification processing.

Instead of executing provider requests directly inside the notification ingestion API, notification events are persisted and processed asynchronously.

### Messaging Workflow

1. A client submits a notification request.
2. NotifyX validates and persists the notification.
3. An outbox event is created as part of the persistence workflow.
4. The outbox publisher publishes eligible events to RabbitMQ.
5. RabbitMQ routes the message to the notification processing queue.
6. A consumer receives the notification event.
7. The delivery module selects the appropriate provider.
8. The delivery result is persisted.

### Priority-Based Processing

The messaging infrastructure uses a durable RabbitMQ queue configured with message priority support.

The queue is configured with a maximum priority of four.

This allows messages to be processed according to their assigned priority rather than treating every notification as an identical request.

Priority assignment is integrated with the notification and plan-management workflow.

### RabbitMQ Configuration

The current topology includes:

| Component | Name |
|-----------|------|
| Exchange | `notifyx.notification.exchange` |
| Queue | `notifyx.notification.queue` |
| Routing Key | `notification` |
| Dead Letter Exchange | `notifyx.notification.dlx` |
| Dead Letter Queue | `notifyx.notification.dlq` |
| Maximum Priority | `4` |

The consumer uses manual message acknowledgement and a configured prefetch limit.

---

## 🔄 Transactional Outbox Pattern

NotifyX implements the **Transactional Outbox Pattern** to improve reliability between PostgreSQL and RabbitMQ.

A common problem with asynchronous systems is that database persistence and message publishing are two separate operations.

For example:

- A notification is successfully saved in PostgreSQL.
- RabbitMQ publishing fails.
- The notification exists in the database but is never processed.

The outbox pattern addresses this problem by storing the notification event in the database within the same transaction as the business operation.

### Workflow

```text
Notification Request
        |
        v
PostgreSQL Transaction
        |
        +---- Save Notification
        |
        +---- Save Outbox Event
        |
        v
Transaction Commit
        |
        v
Outbox Publisher
        |
        v
RabbitMQ
        |
        v
Notification Consumer
```

The outbox publisher processes pending events and publishes eligible events to RabbitMQ.

This also provides the foundation for delayed notification processing and scheduled retry events.

---

## 📧 Notification Delivery System

The delivery module is responsible for communicating with external notification providers.

NotifyX uses a provider abstraction so that channel-specific integrations can be added without coupling the notification processing workflow to a particular vendor.

### Provider Abstraction

The current provider contract is:

```java
public interface NotificationProvider {

    NotificationChannel getChannel();

    DeliveryResult send(DeliveryContext context);
}
```

The delivery context contains the information needed to execute a delivery attempt, including:

- Notification identifier
- Project identifier
- Delivery attempt identifier
- Event identifier
- Notification channel
- Priority
- Recipient
- Subject and content
- Metadata
- Project and organization details

### Current Provider Integration

**Brevo Email Provider**

The current email provider integrates with Brevo's transactional email API.

Responsibilities include:

- Validate provider configuration
- Render notification email templates
- Build the provider request
- Submit transactional emails
- Capture the provider message identifier
- Return the delivery result

The provider abstraction allows other integrations, such as SMS and push providers, to be added in the future.

---

## 🔁 Delivery Tracking & Retry Mechanism

NotifyX maintains delivery attempt records to track provider execution and notification outcomes.

A notification can have multiple delivery attempts, allowing the system to retain failure history rather than overwriting previous execution results.

### Delivery Attempt Tracking

Each attempt records information such as:

- Notification reference
- Attempt number
- Event identifier
- Delivery status
- Error details
- Completion timestamp

### Automatic Retry

NotifyX implements a configurable retry policy for failed delivery attempts.

The retry policy currently supports:

- Maximum delivery attempts
- Configurable initial delay
- Configurable maximum delay
- Exponential backoff
- Randomized jitter
- Retry classification based on provider exceptions and response codes

### Current Default Retry Configuration

| Setting | Default Value |
|---------|---------------|
| Maximum Attempts | 5 |
| Initial Delay | 30 seconds |
| Maximum Delay | 3600 seconds |
| Backoff Strategy | Exponential Backoff |
| Jitter | Enabled |

The retry service records failed delivery attempts and creates a new outbox event with a future execution time when another attempt is allowed.

This keeps retry scheduling within the existing outbox and RabbitMQ processing architecture.

---

## 🔔 Webhook Integration

NotifyX includes webhook handling for external provider delivery updates.

The current integration includes Brevo webhook support.

Provider callbacks can be used to update delivery tracking based on events received from the external provider.

This separates provider acceptance from subsequent delivery-status information.

---

## 💳 Plans, Subscriptions & Payments

NotifyX contains dedicated modules for subscription management, plan entitlements, and payment processing.

The platform is designed around usage limits and capabilities associated with organization plans.

### Plan Management

Plan management is responsible for:

- Organization plan association
- Plan entitlements
- Notification usage limits
- Channel-related capabilities
- Usage enforcement

Each organization operates under a plan that determines its available platform resources.

### Payment Integration

The payment module contains Razorpay integration components.

The platform is intended to support one-time plan purchases and upgrades.

Payment verification and entitlement activation are handled through the payment and subscription workflows.

---

## 📊 Analytics & Usage Tracking

The analytics module provides visibility into notification usage and delivery outcomes.

It is designed to help organizations understand how their applications use the notification platform.

### Analytics Responsibilities

- Notification volume tracking
- Delivery status tracking
- Usage statistics
- Project-level notification visibility
- Organization-level usage reporting

Analytics can be used to support plan enforcement, monitoring, and operational reporting.

---

## 🩺 Health Monitoring & Observability

NotifyX includes Spring Boot Actuator for application monitoring.

The health module provides operational health-related functionality.

The platform also uses structured delivery states and error tracking to support troubleshooting.

Key areas for monitoring include:

- Application health
- Database connectivity
- Redis connectivity
- RabbitMQ connectivity
- Notification processing
- Delivery failures
- Retry activity
- Queue and dead-letter activity

---

## 🖥️ Technology Stack

### Backend

| Technology | Purpose |
|------------|---------|
| Java 25 | Programming Language |
| Spring Boot 4.1.2 | Backend Framework |
| Spring Security | Authentication & Authorization |
| Spring Data JPA | Persistence |
| Spring Data Redis | Redis Integration |
| Spring AMQP | RabbitMQ Integration |
| JJWT | JWT Authentication |
| Jakarta Validation | Request Validation |
| Lombok | Boilerplate Reduction |
| Spring Boot Actuator | Application Monitoring |
| Springdoc OpenAPI | API Documentation |

### Database & Infrastructure

| Technology | Purpose |
|------------|---------|
| PostgreSQL | Primary Relational Database |
| Redis | Rate Limiting and Distributed State |
| RabbitMQ | Asynchronous Messaging |
| Brevo | Transactional Email Delivery |
| Razorpay | Payment Integration |

### Frontend

- React
- TypeScript
- Vite

---

## 🧪 Testing Strategy

The project includes tests for important delivery and retry components.

Current testing includes:

- Delivery retry policy tests
- Delivery retry service tests
- Spring Boot application tests

Further testing should cover:

- Authentication and authorization
- API key validation
- Organization and project isolation
- Notification ingestion
- Idempotency
- Rate-limit enforcement
- Transactional outbox publishing
- RabbitMQ consumer behaviour
- Provider integration
- Webhook processing
- Payment verification
- End-to-end notification delivery

---

## 📁 Project Structure

```text
notifyX/
|
+-- notifyx/
|   |
|   +-- src/main/java/com/beanstack/notifyx/
|       |
|       +-- identity/
|       +-- authorization/
|       +-- organization/
|       +-- project/
|       +-- apikey/
|       +-- notification/
|       +-- ratelimit/
|       +-- messaging/
|       +-- outbox/
|       +-- delivery/
|       +-- subscription/
|       +-- payment/
|       +-- analytics/
|       +-- health/
|       +-- shared/
|       |
|       +-- NotifyxApplication.java
|   |
|   +-- src/test/
|   +-- pom.xml
|
+-- notifyx-frontend/
|
+-- containerfiles/
|
+-- README.md
```

Each module encapsulates a specific business capability and can be developed independently while remaining part of the same Spring Boot application.

---

## 🛠️ Running the Project Locally

### Prerequisites

- Java 25
- Maven
- PostgreSQL
- Redis
- RabbitMQ
- Node.js (for the frontend)

### Infrastructure

Start the required infrastructure services:

- PostgreSQL
- Redis
- RabbitMQ

The project contains container-related configuration under the `containerfiles/` directory.

### Backend

Navigate to the backend directory:

```bash
cd notifyx
```

Build the application:

```bash
mvn clean install
```

Run the Spring Boot application:

```bash
mvn spring-boot:run
```

### Frontend

Navigate to the frontend directory:

```bash
cd notifyx-frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

**Note:** Configure the database, Redis, RabbitMQ, JWT, and external provider credentials through the application's environment/configuration properties before running the complete workflow. Do not commit production secrets to version control.

---

## 📌 Design Principles

- Modular monolith architecture
- Separation of business capabilities
- Multi-tenant data isolation
- Stateless JWT-based authentication
- Project-scoped API access
- Asynchronous notification processing
- Reliable event publishing using the Outbox Pattern
- Priority-aware message processing
- Idempotent request and consumer handling
- Provider abstraction for extensible delivery channels
- Configurable retry policies
- Centralized usage and entitlement management
- Operational visibility and maintainability

---

## 🚧 Future Enhancements

The following areas are planned or require additional production hardening:

- SMS provider integration
- Push notification provider integration
- In-app notification delivery
- Additional notification providers
- Stronger retry concurrency and duplicate-delivery protection
- Expanded integration and end-to-end test coverage
- Production-grade provider timeout and connection-pool configuration
- Payment and entitlement workflow hardening
- Database migration management
- Enhanced observability and operational dashboards
- Deployment automation and production infrastructure

---

## 📄 License

This project is intended for learning, experimentation, and engineering development. Add an explicit license before permitting reuse or redistribution.
