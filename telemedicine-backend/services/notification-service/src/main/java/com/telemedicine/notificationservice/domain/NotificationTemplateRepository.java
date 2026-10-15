package com.telemedicine.notificationservice.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, String> {

    Optional<NotificationTemplateEntity> findByTemplateKeyAndLocaleAndChannelAndActiveTrue(
            String templateKey, String locale, String channel);
}
