package com.aegisterra.platform.infrastructure.persistence.engagement;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplateEntity extends AuditableEntity {

    @Column(nullable = false, length = 128)
    private String code;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "title_template", nullable = false, length = 500)
    private String titleTemplate;

    @Column(name = "body_template", nullable = false, length = 4000)
    private String bodyTemplate;

    @Column(length = 2000)
    private String description;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
