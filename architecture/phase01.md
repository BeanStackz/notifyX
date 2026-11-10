# NotifyX Modular Monolith Architecture Plan

This document outlines the architectural blueprint, folder structure, module responsibilities, and request processing pipeline for the **NotifyX** application.

---

## Phase 01: Local Infrastructure Setup

### Completed

- Added a Podman Compose configuration at [`containerfiles/postgres-setup.yaml`](../containerfiles/postgres-setup.yaml).
- Configured PostgreSQL 16 with:
  - Database: `notification`
  - Username: `notification`
  - Password: `notification`
  - Port: `5432`
  - Persistent named volume: `notification-data`
  - SCRAM-SHA-256 password authentication
  - Maximum connections: `200`
  - UTC timezone
- Added a PostgreSQL health check using `pg_isready`.
- Configured the application to use the local Spring profile in [`notifyx/src/main/resources/application.properties`](../notifyx/src/main/resources/application.properties).

### Start the Local Database

From the `containerfiles` directory:

```powershell
podman compose -f .\postgres-setup.yaml up -d
```

The database is available at `localhost:5432` for local development.

### Phase 01 Status

The local PostgreSQL infrastructure is ready. Spring datasource properties and application-level database integration remain subsequent tasks.

---

## 1. System Architecture Diagram

```mermaid
graph TD
    Client[HTTP Client] -->|Incoming Request| SecurityFilter[Spring Security Filter Chain]
    SecurityFilter -->|JWT Validation| JwtAuth[Identity Module: JwtAuthenticationFilter]
    JwtAuth -->|SecurityContext Established| RBAC[Authorization Module: RBAC & @PreAuthorize]
    RBAC -->|Authorized| RateLimitInterceptor[Rate Limit Module: Interceptor]
    RateLimitInterceptor -->|Atomic Evaluation| RedisLua[Redis + Lua Script Token Bucket]
    RedisLua -->|Denied / 429| HTTP429[HTTP 429 Too Many Requests]
    RedisLua -->|Allowed| Controller[Feature Controllers / Business Logic]
    Controller --> DB[(PostgreSQL)]