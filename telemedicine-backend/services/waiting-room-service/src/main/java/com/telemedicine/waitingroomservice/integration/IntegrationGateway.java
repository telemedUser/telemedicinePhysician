package com.telemedicine.waitingroomservice.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class IntegrationGateway {

    private static final Logger log = LoggerFactory.getLogger(IntegrationGateway.class);

    private static final ParameterizedTypeReference<ApiResponse<IntegrationAppointment>> APPOINTMENT_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient integrationRestClient;

    public IntegrationGateway(RestClient integrationRestClient) {
        this.integrationRestClient = integrationRestClient;
    }

    public boolean isPaymentConfirmed(String appointmentId) {
        try {
            IntegrationAppointment appointment = consultAppointment(appointmentId);
            return appointment != null
                    && "CONFIRMED".equalsIgnoreCase(appointment.paymentStatus());
        } catch (RestClientException exception) {
            log.warn(
                    "Payment status lookup failed for {}: {}",
                    appointmentId,
                    exception.getMessage());
            return false;
        }
    }

    public void requirePaymentConfirmed(String appointmentId) {
        if (!isPaymentConfirmed(appointmentId)) {
            try {
                validatePayment(appointmentId);
            } catch (RestClientException exception) {
                throw new DomainException(
                        ErrorCode.PAYMENT_NOT_CONFIRMED,
                        "Payment is not confirmed for appointment " + appointmentId);
            }
        }
    }

    public IntegrationAppointment consultAppointment(String appointmentId) {
        ApiResponse<IntegrationAppointment> response =
                integrationRestClient
                        .get()
                        .uri("/v1/appointments/v2/{id}", appointmentId)
                        .retrieve()
                        .body(APPOINTMENT_TYPE);
        if (response == null || response.data() == null) {
            throw new DomainException(
                    ErrorCode.APPOINTMENT_NOT_FOUND, "Appointment not found: " + appointmentId);
        }
        return response.data();
    }

    public IntegrationAppointment consultAppointmentFailOpen(String appointmentId) {
        try {
            return consultAppointment(appointmentId);
        } catch (RestClientException exception) {
            log.warn("Integration consult failed for {}: {}", appointmentId, exception.getMessage());
            return null;
        }
    }

    public void validatePayment(String appointmentId) {
        ApiResponse<IntegrationAppointment> response =
                integrationRestClient
                        .post()
                        .uri("/v1/appointments/v2/{id}/payment/validate", appointmentId)
                        .retrieve()
                        .body(APPOINTMENT_TYPE);
        if (response == null
                || response.data() == null
                || !"CONFIRMED".equalsIgnoreCase(response.data().paymentStatus())) {
            throw new DomainException(
                    ErrorCode.PAYMENT_NOT_CONFIRMED,
                    "Payment is not confirmed for appointment " + appointmentId);
        }
    }

    public void syncHisStatus(String appointmentId, String status) {
        try {
            integrationRestClient
                    .put()
                    .uri("/v1/appointments/v2/{id}/status", appointmentId)
                    .body(java.util.Map.of("status", status))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn(
                    "HIS status sync failed for {} -> {}: {}",
                    appointmentId,
                    status,
                    exception.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IntegrationAppointment(
            String id,
            String patientId,
            String physicianId,
            String status,
            String scheduledAt,
            String paymentStatus,
            String source) {}
}
