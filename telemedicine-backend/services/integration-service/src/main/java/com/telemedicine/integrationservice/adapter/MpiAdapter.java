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
public class MpiAdapter
        extends ExternalSystemAdapter<MpiAdapter.LookupRequest, String, Map<String, Object>, MpiAdapter.PatientRecord> {

    private final RestClient client;

    public MpiAdapter(
            IntegrationHttpClientFactory clientFactory,
            IntegrationFailureReporter failureReporter,
            TelemedicineIntegrationProperties properties) {
        super(ExternalSystems.MPI, failureReporter, MpiAdapter.mapper());
        this.client =
                clientFactory.restClient(
                        AdapterSupport.trimTrailingSlash(properties.getSimulator().getBaseUrl())
                                + "/sim/mpi");
    }

    public PatientRecord lookup(String patientRef) {
        return execute(new LookupRequest(patientRef), "patientLookup", patientRef);
    }

    @Override
    protected Map<String, Object> invoke(String patientRef) {
        return client.get()
                .uri("/patients/{patientRef}", patientRef)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private static AntiCorruptionMapper<LookupRequest, String, Map<String, Object>, PatientRecord> mapper() {
        return new AntiCorruptionMapper<>() {
            @Override
            public String toExternal(LookupRequest domainRequest) {
                return domainRequest.patientRef();
            }

            @Override
            public PatientRecord toDomain(Map<String, Object> externalResponse) {
                return new PatientRecord(
                        stringValue(externalResponse, "patientRef"),
                        stringValue(externalResponse, "name"),
                        stringValue(externalResponse, "dateOfBirth"),
                        stringValue(externalResponse, "nationalIdHash"));
            }
        };
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    public record LookupRequest(String patientRef) {}

    public record PatientRecord(
            String patientRef, String name, String dateOfBirth, String nationalIdHash) {}
}
