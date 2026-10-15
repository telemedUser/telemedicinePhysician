package com.telemedicine.integrationservice.service;

import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.integrationservice.adapter.PaymentAdapter;
import com.telemedicine.integrationservice.adapter.SapAdapter;
import com.telemedicine.integrationservice.config.IntegrationServiceProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Profile("postgres")
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final SapAdapter sapAdapter;
    private final PaymentAdapter paymentAdapter;
    private final RestClient waitingRoomClient;

    public WebhookService(
            SapAdapter sapAdapter,
            PaymentAdapter paymentAdapter,
            IntegrationServiceProperties properties,
            org.springframework.web.client.RestClient.Builder restClientBuilder) {
        this.sapAdapter = sapAdapter;
        this.paymentAdapter = paymentAdapter;
        this.waitingRoomClient = restClientBuilder.baseUrl(properties.getWaitingRoomBaseUrl()).build();
    }

    public Map<String, String> handleSapLinkRequest(Map<String, String> payload) {
        String appointmentId = payload.get("appointmentId");
        if (appointmentId == null || appointmentId.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "appointmentId is required");
        }

        SapAdapter.VerifyResult verification = sapAdapter.verifyAppointment(appointmentId);
        if (!verification.valid()) {
            throw new DomainException(
                    ErrorCode.APPOINTMENT_NOT_ELIGIBLE, "SAP appointment verification failed: " + appointmentId);
        }

        PaymentAdapter.ValidationResult payment = paymentAdapter.validate(appointmentId);
        if (!payment.paid()) {
            throw new DomainException(ErrorCode.PAYMENT_NOT_CONFIRMED, "Payment not confirmed: " + appointmentId);
        }

        SapAdapter.LinkRequestResult linkRequest = sapAdapter.linkRequest(appointmentId);
        try {
            waitingRoomClient
                    .post()
                    .uri("/api/v1/waiting-room/session-link")
                    .body(
                            Map.of(
                                    "appointmentId", appointmentId,
                                    "linkToken", linkRequest.linkToken()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RuntimeException exception) {
            log.warn("Waiting-room session-link call failed for appointment={}", appointmentId, exception);
            throw new DomainException(
                    ErrorCode.INTEGRATION_ERROR,
                    "Failed to create waiting-room session link for: " + appointmentId);
        }

        return Map.of(
                "appointmentId", appointmentId,
                "linkToken", linkRequest.linkToken(),
                "status", "ACCEPTED");
    }
}
