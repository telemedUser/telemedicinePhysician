package com.telemedicine.common.security.consent;

import com.telemedicine.common.core.dto.ApiResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Checks consent by calling waiting-room-service. Used by identity, video, and telemetry services.
 */
public class HttpConsentVerifier implements ConsentVerifier {

    private static final Logger log = LoggerFactory.getLogger(HttpConsentVerifier.class);

    private static final ParameterizedTypeReference<ApiResponse<Map<String, Object>>> CONSENT_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient waitingRoomRestClient;

    public HttpConsentVerifier(RestClient waitingRoomRestClient) {
        this.waitingRoomRestClient = waitingRoomRestClient;
    }

    @Override
    public ConsentDecision verify(String subjectId, String consentType, String requiredVersion) {
        try {
            ApiResponse<Map<String, Object>> response =
                    waitingRoomRestClient
                            .get()
                            .uri(
                                    "/api/v1/waiting-room/consent/{subjectId}?type={type}&version={version}",
                                    subjectId,
                                    consentType,
                                    requiredVersion != null ? requiredVersion : "")
                            .retrieve()
                            .body(CONSENT_TYPE);
            if (response == null || response.data() == null) {
                return ConsentDecision.missing();
            }
            Map<String, Object> data = response.data();
            boolean granted = Boolean.TRUE.equals(data.get("granted"));
            if (granted) {
                return ConsentDecision.grant();
            }
            String reason = String.valueOf(data.getOrDefault("reason", "CONSENT_REQUIRED"));
            return switch (reason) {
                case "CONSENT_WITHDRAWN" -> ConsentDecision.withdrawn();
                case "CONSENT_VERSION_OUTDATED" ->
                        ConsentDecision.outdated(String.valueOf(data.get("requiredVersion")));
                default -> ConsentDecision.missing();
            };
        } catch (RestClientException exception) {
            log.warn("Consent verification failed for subject {}: {}", subjectId, exception.getMessage());
            return ConsentDecision.missing();
        }
    }
}
