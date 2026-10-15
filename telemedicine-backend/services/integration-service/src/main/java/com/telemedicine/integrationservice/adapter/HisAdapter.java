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
public class HisAdapter
        extends ExternalSystemAdapter<
                HisAdapter.GetAppointmentRequest,
                String,
                Map<String, Object>,
                HisAdapter.HisAppointment> {

    private final RestClient client;
    private final IntegrationFailureReporter failureReporter;

    public HisAdapter(
            IntegrationHttpClientFactory clientFactory,
            IntegrationFailureReporter failureReporter,
            TelemedicineIntegrationProperties properties) {
        super(ExternalSystems.HIS, failureReporter, HisAdapter.mapper());
        this.failureReporter = failureReporter;
        this.client =
                clientFactory.restClient(
                        AdapterSupport.trimTrailingSlash(properties.getSimulator().getBaseUrl())
                                + "/sim/his");
    }

    public HisAppointment getAppointment(String appointmentId) {
        return execute(new GetAppointmentRequest(appointmentId), "getAppointment", appointmentId);
    }

    public HisStatusUpdate updateStatus(String appointmentId, String status) {
        return AdapterSupport.execute(
                failureReporter,
                ExternalSystems.HIS,
                "updateStatus",
                appointmentId,
                () -> {
                    Map<String, String> response =
                            client.post()
                                    .uri("/appointments/{id}/status", appointmentId)
                                    .body(Map.of("status", status))
                                    .retrieve()
                                    .body(new ParameterizedTypeReference<Map<String, String>>() {});
                    return new HisStatusUpdate(
                            response.getOrDefault("appointmentId", appointmentId),
                            response.getOrDefault("status", status));
                });
    }

    @Override
    protected Map<String, Object> invoke(String appointmentId) {
        return client.get()
                .uri("/appointments/{id}", appointmentId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private static AntiCorruptionMapper<
                    GetAppointmentRequest, String, Map<String, Object>, HisAppointment>
            mapper() {
        return new AntiCorruptionMapper<>() {
            @Override
            public String toExternal(GetAppointmentRequest domainRequest) {
                return domainRequest.appointmentId();
            }

            @Override
            public HisAppointment toDomain(Map<String, Object> externalResponse) {
                return new HisAppointment(
                        stringValue(externalResponse, "appointmentId"),
                        stringValue(externalResponse, "status"),
                        stringValue(externalResponse, "patientId"));
            }
        };
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    public record GetAppointmentRequest(String appointmentId) {}

    public record HisAppointment(String appointmentId, String status, String patientId) {}

    public record HisStatusUpdate(String appointmentId, String status) {}
}
