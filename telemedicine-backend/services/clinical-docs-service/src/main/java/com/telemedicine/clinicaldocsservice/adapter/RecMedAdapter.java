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
public class RecMedAdapter
        extends ExternalSystemAdapter<
                RecMedPrescriptionRequest, Map<String, Object>, Map<String, String>, RecMedPrescription> {

    private static final ParameterizedTypeReference<Map<String, String>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final IntegrationHttpClientFactory clientFactory;
    private final TelemedicineIntegrationProperties properties;

    public RecMedAdapter(
            IntegrationFailureReporter failureReporter,
            IntegrationHttpClientFactory clientFactory,
            TelemedicineIntegrationProperties properties) {
        super(
                ExternalSystems.RECMED,
                failureReporter,
                new AntiCorruptionMapper<>() {
                    @Override
                    public Map<String, Object> toExternal(RecMedPrescriptionRequest domainRequest) {
                        return Map.of(
                                "appointmentId", domainRequest.appointmentId(),
                                "medications", domainRequest.medications());
                    }

                    @Override
                    public RecMedPrescription toDomain(Map<String, String> externalResponse) {
                        return new RecMedPrescription(externalResponse.get("prescriptionId"));
                    }
                });
        this.clientFactory = clientFactory;
        this.properties = properties;
    }

    public RecMedPrescription issuePrescription(String appointmentId, String medications) {
        return execute(new RecMedPrescriptionRequest(appointmentId, medications), "prescription", appointmentId);
    }

    @Override
    protected Map<String, String> invoke(Map<String, Object> externalRequest) {
        Map<String, String> response =
                client().post().uri("/prescriptions").body(externalRequest).retrieve().body(MAP_TYPE);
        if (response == null || response.get("prescriptionId") == null) {
            throw new IllegalStateException("RecMed prescription response was empty");
        }
        return response;
    }

    @Override
    protected ErrorCode mapErrorCode(RuntimeException exception) {
        ErrorCode mapped = super.mapErrorCode(exception);
        if (mapped == ErrorCode.INTEGRATION_ERROR || mapped == ErrorCode.EXTERNAL_TIMEOUT) {
            return ErrorCode.PRESCRIPTION_ISSUE_FAILED;
        }
        return mapped;
    }

    private org.springframework.web.client.RestClient client() {
        return clientFactory.restClient(simulatorBaseUrl() + "/sim/recmed");
    }

    private String simulatorBaseUrl() {
        String baseUrl = properties.getSimulator().getBaseUrl();
        if (baseUrl.startsWith("lb://")) {
            return baseUrl.replace("lb://", "http://");
        }
        return baseUrl;
    }
}
