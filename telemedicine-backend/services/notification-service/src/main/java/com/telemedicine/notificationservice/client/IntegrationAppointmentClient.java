package com.telemedicine.notificationservice.client;

import com.telemedicine.common.core.dto.ApiResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class IntegrationAppointmentClient {

    private static final Logger log = LoggerFactory.getLogger(IntegrationAppointmentClient.class);

    private static final ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>> LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public IntegrationAppointmentClient(RestClient integrationServiceRestClient) {
        this.restClient = integrationServiceRestClient;
    }

    public List<ReminderAppointment> upcomingReminders(int windowHours) {
        try {
            ApiResponse<List<Map<String, Object>>> response =
                    restClient
                            .get()
                            .uri(
                                    "/api/v1/integration/appointments/reminders?windowHours={hours}",
                                    windowHours)
                            .retrieve()
                            .body(LIST_TYPE);
            if (response == null || response.data() == null) {
                return List.of();
            }
            return response.data().stream().map(IntegrationAppointmentClient::toReminder).toList();
        } catch (RestClientException exception) {
            log.warn("Integration reminders lookup failed (fail-open): {}", exception.getMessage());
            return Collections.emptyList();
        }
    }

    private static ReminderAppointment toReminder(Map<String, Object> row) {
        return new ReminderAppointment(
                String.valueOf(row.get("appointmentId")),
                String.valueOf(row.getOrDefault("patientRef", row.get("patientId"))),
                String.valueOf(row.getOrDefault("scheduledAt", "")));
    }

    public record ReminderAppointment(String appointmentId, String patientRef, String scheduledAt) {}
}
