package com.telemedicine.notificationservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "templates")
public class NotificationTemplateEntity {

    @Id
    private String id;

    @Column(name = "template_key", nullable = false)
    private String templateKey;

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false)
    private String channel;

    private String subject;

    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    private boolean active;

    protected NotificationTemplateEntity() {}

    public NotificationTemplateEntity(
            String id,
            String templateKey,
            String locale,
            String channel,
            String subject,
            String body,
            boolean active) {
        this.id = id;
        this.templateKey = templateKey;
        this.locale = locale;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public String getLocale() {
        return locale;
    }

    public String getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public boolean isActive() {
        return active;
    }
}
