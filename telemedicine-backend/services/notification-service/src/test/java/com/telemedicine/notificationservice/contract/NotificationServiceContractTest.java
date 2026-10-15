package com.telemedicine.notificationservice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NotificationServiceContractTest {

    @Test
    void contractDocumentsNotificationEndpoints() throws Exception {
        Path contract =
                Path.of(System.getProperty("telemedicine.contracts.dir", ""))
                        .resolve("notification-service.yaml");
        String spec = Files.readString(contract);
        assertThat(spec).contains("/api/v1/notifications/send");
        assertThat(spec).contains("/api/v1/internal/jobs/appointment-reminders/run");
    }
}
