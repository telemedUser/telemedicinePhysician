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
public class PaymentAdapter
        extends ExternalSystemAdapter<
                PaymentAdapter.ValidateRequest, String, Map<String, Object>, PaymentAdapter.ValidationResult> {

    private final RestClient client;

    public PaymentAdapter(
            IntegrationHttpClientFactory clientFactory,
            IntegrationFailureReporter failureReporter,
            TelemedicineIntegrationProperties properties) {
        super(ExternalSystems.PAYMENT, failureReporter, PaymentAdapter.mapper());
        this.client =
                clientFactory.restClient(
                        AdapterSupport.trimTrailingSlash(properties.getSimulator().getBaseUrl())
                                + "/sim/payment");
    }

    public ValidationResult validate(String appointmentId) {
        return execute(new ValidateRequest(appointmentId), "validatePayment", appointmentId);
    }

    @Override
    protected Map<String, Object> invoke(String appointmentId) {
        return client.get()
                .uri("/validate/{appointmentId}", appointmentId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private static AntiCorruptionMapper<
                    ValidateRequest, String, Map<String, Object>, ValidationResult>
            mapper() {
        return new AntiCorruptionMapper<>() {
            @Override
            public String toExternal(ValidateRequest domainRequest) {
                return domainRequest.appointmentId();
            }

            @Override
            public ValidationResult toDomain(Map<String, Object> externalResponse) {
                Object paid = externalResponse.get("paid");
                boolean isPaid = paid instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(paid));
                return new ValidationResult(
                        stringValue(externalResponse, "appointmentId"),
                        isPaid,
                        stringValue(externalResponse, "transactionId"));
            }
        };
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    public record ValidateRequest(String appointmentId) {}

    public record ValidationResult(String appointmentId, boolean paid, String transactionId) {}
}
