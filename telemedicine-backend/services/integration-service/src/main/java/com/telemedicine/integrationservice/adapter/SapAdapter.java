package com.telemedicine.integrationservice.adapter;

import com.telemedicine.common.integration.ExternalSystems;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.acl.AntiCorruptionMapper;
import com.telemedicine.common.integration.acl.ExternalSystemAdapter;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("postgres")
public class SapAdapter
        extends ExternalSystemAdapter<SapAdapter.VerifyRequest, String, Map<String, Object>, SapAdapter.VerifyResult> {

    private final RestClient client;
    private final IntegrationFailureReporter failureReporter;

    public SapAdapter(
            IntegrationHttpClientFactory clientFactory,
            IntegrationFailureReporter failureReporter,
            TelemedicineIntegrationProperties properties) {
        super(ExternalSystems.SAP, failureReporter, SapAdapter.mapper());
        this.failureReporter = failureReporter;
        this.client =
                clientFactory.restClient(
                        AdapterSupport.trimTrailingSlash(properties.getSimulator().getBaseUrl())
                                + "/sim/sap");
    }

    public VerifyResult verifyAppointment(String appointmentId) {
        return execute(new VerifyRequest(appointmentId), "verifyAppointment", appointmentId);
    }

    public LinkRequestResult linkRequest(String appointmentId) {
        return AdapterSupport.execute(
                failureReporter,
                ExternalSystems.SAP,
                "linkRequest",
                appointmentId,
                () -> {
                    Map<String, String> response =
                            client.post()
                                    .uri("/link-request")
                                    .body(Map.of("appointmentId", appointmentId))
                                    .retrieve()
                                    .body(new ParameterizedTypeReference<Map<String, String>>() {});
                    return new LinkRequestResult(
                            response.get("appointmentId"), response.get("linkToken"));
                });
    }

    @Override
    protected Map<String, Object> invoke(String appointmentId) {
        return client.get()
                .uri("/appointments/{id}/verify", appointmentId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private static AntiCorruptionMapper<VerifyRequest, String, Map<String, Object>, VerifyResult> mapper() {
        return new AntiCorruptionMapper<>() {
            @Override
            public String toExternal(VerifyRequest domainRequest) {
                return domainRequest.appointmentId();
            }

            @Override
            public VerifyResult toDomain(Map<String, Object> externalResponse) {
                Object valid = externalResponse.get("valid");
                boolean isValid = valid instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(valid));
                return new VerifyResult(stringValue(externalResponse, "appointmentId"), isValid);
            }
        };
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    public record VerifyRequest(String appointmentId) {}

    public record VerifyResult(String appointmentId, boolean valid) {}

    public record LinkRequestResult(String appointmentId, String linkToken) {}
}
