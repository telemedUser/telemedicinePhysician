package com.telemedicine.clinicaldocsservice.adapter;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.integration.ExternalSystems;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.acl.AntiCorruptionMapper;
import com.telemedicine.common.integration.acl.ExternalSystemAdapter;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class MedipassAdapter
        extends ExternalSystemAdapter<
                MedipassCertificateRequest, Map<String, Object>, Map<String, String>, MedipassCertificate> {

    private static final ParameterizedTypeReference<Map<String, String>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final IntegrationHttpClientFactory clientFactory;
    private final TelemedicineIntegrationProperties properties;

    public MedipassAdapter(
            IntegrationFailureReporter failureReporter,
            IntegrationHttpClientFactory clientFactory,
            TelemedicineIntegrationProperties properties) {
        super(
                ExternalSystems.MEDIPASS,
                failureReporter,
                new AntiCorruptionMapper<>() {
                    @Override
                    public Map<String, Object> toExternal(MedipassCertificateRequest domainRequest) {
                        return Map.of(
                                "appointmentId", domainRequest.appointmentId(),
                                "durationDays", domainRequest.durationDays(),
                                "diagnosis", domainRequest.diagnosis());
                    }

                    @Override
                    public MedipassCertificate toDomain(Map<String, String> externalResponse) {
                        return new MedipassCertificate(externalResponse.get("certificateId"));
                    }
                });
        this.clientFactory = clientFactory;
        this.properties = properties;
    }

    public MedipassCertificate issueCertificate(String appointmentId, int durationDays, String diagnosis) {
        return execute(
                new MedipassCertificateRequest(appointmentId, durationDays, diagnosis),
                "certificate",
                appointmentId);
    }

    @Override
    protected Map<String, String> invoke(Map<String, Object> externalRequest) {
        Map<String, String> response =
                client().post().uri("/certificates").body(externalRequest).retrieve().body(MAP_TYPE);
        if (response == null || response.get("certificateId") == null) {
            throw new IllegalStateException("Medipass certificate response was empty");
        }
        return response;
    }

    @Override
    protected ErrorCode mapErrorCode(RuntimeException exception) {
        ErrorCode mapped = super.mapErrorCode(exception);
        if (mapped == ErrorCode.INTEGRATION_ERROR || mapped == ErrorCode.EXTERNAL_TIMEOUT) {
            return ErrorCode.DOCUMENT_GENERATION_FAILED;
        }
        return mapped;
    }

    private org.springframework.web.client.RestClient client() {
        return clientFactory.restClient(simulatorBaseUrl() + "/sim/medipass");
    }

    private String simulatorBaseUrl() {
        String baseUrl = properties.getSimulator().getBaseUrl();
        if (baseUrl.startsWith("lb://")) {
            return baseUrl.replace("lb://", "http://");
        }
        return baseUrl;
    }
}
