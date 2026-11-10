# NotifyX Modular Monolith Architecture Plan

This document outlines the architectural blueprint, folder structure, module responsibilities, and request processing pipeline for the **NotifyX** application.

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