package com.aegisterra.platform.application.notification;

import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationTemplateEntity;
import com.aegisterra.platform.infrastructure.persistence.engagement.NotificationTemplateRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;

    public TemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public RenderedTemplate render(String code, String channel, Map<String, String> vars) {
        NotificationTemplateEntity template = templateRepository
            .findByCodeAndChannelAndLocaleAndDeletedFalse(code, channel, "en")
            .orElse(null);
        if (template == null) {
            return new RenderedTemplate(
                code,
                "Notification",
                vars.getOrDefault("summary", "You have a new notification.")
            );
        }
        return new RenderedTemplate(
            template.getCode(),
            replace(template.getTitleTemplate(), vars),
            replace(template.getBodyTemplate(), vars)
        );
    }

    static String replace(String body, Map<String, String> vars) {
        String result = body;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    public record RenderedTemplate(String templateCode, String title, String body) {}
}
