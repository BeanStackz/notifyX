
-- ============================================================
-- NOTIFYX DATABASE SCHEMA
-- PostgreSQL
-- Based on repository JPA entities and DATABASE_DESIGN.md
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 1. IDENTITY AND AUTHENTICATION
-- ============================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    used_at TIMESTAMPTZ,
    replaced_by_token_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_refresh_tokens_user
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_family
    ON refresh_tokens(family_id);


-- ============================================================
-- 2. ORGANIZATION AND MULTI-TENANCY
-- ============================================================

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_organizations_slug
    ON organizations(slug);

CREATE TABLE organization_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_org_member_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_org_member_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uk_organization_member
        UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_org_members_organization
    ON organization_members(organization_id);

CREATE INDEX idx_org_members_user
    ON organization_members(user_id);


-- ============================================================
-- 3. PROJECTS AND API KEY MANAGEMENT
-- ============================================================

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_project_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uk_project_organization_slug
        UNIQUE (organization_id, slug)
);

CREATE INDEX idx_projects_organization
    ON projects(organization_id);

CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    key_id VARCHAR(16) NOT NULL UNIQUE,
    key_hash VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NOT NULL,
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_api_key_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id),

    CONSTRAINT fk_api_key_creator
        FOREIGN KEY (created_by)
        REFERENCES users(id)
);

CREATE INDEX idx_api_keys_key_id
    ON api_keys(key_id);

CREATE INDEX idx_api_keys_project_id
    ON api_keys(project_id);


-- ============================================================
-- 4. SUBSCRIPTION AND PLAN MANAGEMENT
-- ============================================================

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    monthly_quota BIGINT NOT NULL,
    rate_limit_per_minute INTEGER NOT NULL,
    monthly_price_in_paise BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    razorpay_plan_id VARCHAR(50) UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE plan_allowed_channels (
    plan_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,

    PRIMARY KEY (plan_id, channel),

    CONSTRAINT fk_plan_allowed_channels
        FOREIGN KEY (plan_id)
        REFERENCES subscription_plans(id)
);

CREATE TABLE organization_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    razorpay_subscription_id VARCHAR(50) UNIQUE,
    authorization_payment_id VARCHAR(50),
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_subscription_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_subscription_plan
        FOREIGN KEY (plan_id)
        REFERENCES subscription_plans(id)
);

CREATE INDEX idx_subscription_organization
    ON organization_subscriptions(organization_id);

CREATE INDEX idx_subscription_status
    ON organization_subscriptions(status);

CREATE TABLE subscription_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    notifications_used BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_usage_subscription
        FOREIGN KEY (subscription_id)
        REFERENCES organization_subscriptions(id),

    CONSTRAINT uk_subscription_usage_period
        UNIQUE (subscription_id, period_start)
);


-- ============================================================
-- 5. NOTIFICATION MANAGEMENT
-- ============================================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    subject VARCHAR(255),
    content TEXT NOT NULL,
    metadata JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    scheduled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notification_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id),

    CONSTRAINT uk_notification_project_idempotency
        UNIQUE (project_id, idempotency_key)
);

CREATE INDEX idx_notification_project
    ON notifications(project_id);

CREATE INDEX idx_notification_status
    ON notifications(status);

CREATE INDEX idx_notification_created
    ON notifications(created_at);

CREATE INDEX idx_notification_scheduled
    ON notifications(scheduled_at);


-- ============================================================
-- 6. TRANSACTIONAL OUTBOX
-- ============================================================

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(100),
    last_error TEXT,
    event_version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_outbox_status_created
    ON outbox_events(status, created_at);

CREATE INDEX idx_outbox_aggregate
    ON outbox_events(aggregate_id);

CREATE INDEX idx_outbox_claim
    ON outbox_events(status, next_attempt_at, created_at);

CREATE INDEX idx_outbox_stale
    ON outbox_events(status, locked_at);


-- ============================================================
-- 7. CONSUMER IDEMPOTENCY
-- ============================================================

CREATE TABLE notification_consumer_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(100) NOT NULL,
    notification_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    last_error TEXT,

    CONSTRAINT fk_consumer_event_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id),

    CONSTRAINT uk_notification_consumer_event
        UNIQUE (event_id)
);


-- ============================================================
-- 8. DELIVERY ATTEMPTS
-- ============================================================

CREATE TABLE delivery_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    provider_message_id VARCHAR(255),
    attempt_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_response TEXT,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_delivery_attempt_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id),

    CONSTRAINT uk_delivery_notification_attempt
        UNIQUE (notification_id, attempt_number)
);

CREATE INDEX idx_delivery_message
    ON delivery_attempts(provider_message_id);


-- ============================================================
-- 9. DELIVERY EVENTS / BREVO WEBHOOK ANALYTICS
-- ============================================================

CREATE TABLE delivery_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    provider_message_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    recipient VARCHAR(320),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_delivery_event_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id),

    CONSTRAINT uk_delivery_provider_event
        UNIQUE (provider_message_id, event_type)
);

CREATE INDEX idx_delivery_event_notification
    ON delivery_events(notification_id);

CREATE INDEX idx_delivery_events_message
    ON delivery_events(provider_message_id, event_type);


-- ============================================================
-- 10. BILLING ORDERS AND PAYMENTS
-- ============================================================

CREATE TABLE billing_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    razorpay_order_id VARCHAR(255) UNIQUE,
    razorpay_payment_id VARCHAR(255) UNIQUE,
    amount_in_paise BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ,

    CONSTRAINT fk_billing_order_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_billing_order_plan
        FOREIGN KEY (plan_id)
        REFERENCES subscription_plans(id)
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL,
    razorpay_payment_id VARCHAR(255) NOT NULL UNIQUE,
    amount_in_paise BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payment_subscription
        FOREIGN KEY (subscription_id)
        REFERENCES organization_subscriptions(id)
);

CREATE INDEX idx_payment_subscription
    ON payments(subscription_id);


-- ============================================================
-- 11. RAZORPAY WEBHOOK IDEMPOTENCY
-- ============================================================

CREATE TABLE razorpay_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ
);


-- ============================================================
-- END OF NOTIFYX DATABASE
-- ============================================================
