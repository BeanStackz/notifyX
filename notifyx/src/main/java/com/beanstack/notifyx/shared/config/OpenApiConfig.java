package com.beanstack.notifyx.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "NotifyX Backend API",
        version = "v1",
        description = """
            NotifyX is a multi-tenant notification delivery backend. Create organizations and projects,
            issue project-scoped API keys, and submit idempotent email notifications. Notifications are
            persisted with an outbox event and processed asynchronously through RabbitMQ and the configured
            delivery provider. The API also exposes delivery tracking, usage analytics, billing, and
            operational health information.

            User and management endpoints use JWT bearer tokens. Notification ingestion uses the
            `X-NotifyX-API-Key` project key. Provider webhooks are authenticated with provider-specific
            signatures or secrets. Most successful responses use the common `{ success, message, data,
            timestamp }` envelope; webhook responses are empty.

            A notification's `scheduledAt` value is currently recorded, but does not delay publication.
            See the project API guide for local setup, endpoint groups, and current behavior.
            """
    )
)
@SecuritySchemes({
    @SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT access token. Enter the token without the `Bearer ` prefix."
    ),
    @SecurityScheme(
        name = "apiKeyAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-NotifyX-API-Key",
        description = "Project-scoped API key. Use for notification ingestion endpoints."
    )
})
public class OpenApiConfig {
}
