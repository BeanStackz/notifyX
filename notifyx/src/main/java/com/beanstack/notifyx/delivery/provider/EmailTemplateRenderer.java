package com.beanstack.notifyx.delivery.provider;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.beanstack.notifyx.delivery.service.DeliveryContext;

@Component
public class EmailTemplateRenderer {

    private static final String DEFAULT_TEMPLATE = "professional";

    public String render(DeliveryContext context) {
        String templateId = templateId(context);
        String organization = escape(context.organizationName());
        String project = escape(context.projectName());
        String subject = escape(subject(context));
        String content = escape(context.content()).replace("\r\n", "<br>").replace("\n", "<br>");

        return switch (templateId) {
            case "minimal" -> minimal(organization, project, subject, content);
            case "alert" -> alert(organization, project, subject, content);
            case DEFAULT_TEMPLATE -> professional(organization, project, subject, content);
            default -> throw new IllegalArgumentException("Unsupported email template: " + templateId);
        };
    }

    public String templateId(DeliveryContext context) {
        Object template = context.metadata() == null
            ? null
            : context.metadata().get("emailTemplate");
        if (template instanceof Map<?, ?> values && values.get("id") != null) {
            return values.get("id").toString().trim().toLowerCase();
        }
        return DEFAULT_TEMPLATE;
    }

    private String professional(String organization, String project, String subject, String content) {
        return document("""
            <div class="nx-card">
              <div class="nx-brand">%s</div>
              <div class="nx-kicker">%s notification</div>
              <h1>%s</h1>
              <div class="nx-content">%s</div>
              <div class="nx-footer">Sent by %s via NotifyX</div>
            </div>
            """.formatted(organization, project, subject, content, organization));
    }

    private String minimal(String organization, String project, String subject, String content) {
        return document("""
            <div class="nx-minimal">
              <h1>%s</h1>
              <div class="nx-content">%s</div>
              <p>— %s · %s</p>
            </div>
            """.formatted(subject, content, organization, project));
    }

    private String alert(String organization, String project, String subject, String content) {
        return document("""
            <div class="nx-alert">
              <div class="nx-alert-bar"></div>
              <div class="nx-kicker">%s · %s</div>
              <h1>%s</h1>
              <div class="nx-content">%s</div>
              <div class="nx-footer">This message was sent by %s through NotifyX.</div>
            </div>
            """.formatted(organization, project, subject, content, organization));
    }

    private String document(String body) {
        return """
            <!doctype html>
            <html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              body{margin:0;padding:24px;background:#f3f7f5;color:#30443b;font-family:Arial,sans-serif}
              .nx-card,.nx-minimal,.nx-alert{max-width:620px;margin:auto;background:#fff;border:1px solid #dfeae3;border-radius:16px;padding:32px;box-sizing:border-box}
              .nx-brand{color:#36a979;font-size:20px;font-weight:700;margin-bottom:32px}.nx-kicker{color:#789087;font-size:12px;text-transform:uppercase;letter-spacing:1px}
              h1{font-size:28px;line-height:1.2;margin:10px 0 22px;color:#263a31}.nx-content{font-size:16px;line-height:1.7;white-space:normal}
              .nx-footer{border-top:1px solid #e8efeb;margin-top:30px;padding-top:16px;color:#82948a;font-size:12px}.nx-minimal{padding:24px;border:0;border-top:4px solid #36a979;border-radius:0}.nx-minimal p{color:#82948a;font-size:12px}
              .nx-alert{position:relative;overflow:hidden}.nx-alert-bar{height:5px;background:#ff7b5c;margin:-32px -32px 28px}.nx-alert h1{color:#a34835}
            </style></head><body>%s</body></html>
            """.formatted(body);
    }

    private String subject(DeliveryContext context) {
        return context.subject() == null || context.subject().isBlank()
            ? "NotifyX notification"
            : context.subject();
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
