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
public class ZendeskAdapter
        extends ExternalSystemAdapter<
                ZendeskAdapter.CreateTicketRequest, Map<String, String>, Map<String, String>, ZendeskAdapter.TicketResult> {

    private final RestClient client;

    public ZendeskAdapter(
            IntegrationHttpClientFactory clientFactory,
            IntegrationFailureReporter failureReporter,
            TelemedicineIntegrationProperties properties) {
        super(ExternalSystems.ZENDESK, failureReporter, ZendeskAdapter.mapper());
        this.client =
                clientFactory.restClient(
                        AdapterSupport.trimTrailingSlash(properties.getSimulator().getBaseUrl())
                                + "/sim/zendesk");
    }

    public TicketResult createTicket(String subject, String description, String priority) {
        return execute(
                new CreateTicketRequest(subject, description, priority),
                "createTicket",
                subject);
    }

    @Override
    protected Map<String, String> invoke(Map<String, String> externalRequest) {
        return client.post()
                .uri("/tickets")
                .body(externalRequest)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, String>>() {});
    }

    private static AntiCorruptionMapper<
                    CreateTicketRequest, Map<String, String>, Map<String, String>, TicketResult>
            mapper() {
        return new AntiCorruptionMapper<>() {
            @Override
            public Map<String, String> toExternal(CreateTicketRequest domainRequest) {
                return Map.of(
                        "subject", domainRequest.subject(),
                        "description", domainRequest.description(),
                        "priority", domainRequest.priority());
            }

            @Override
            public TicketResult toDomain(Map<String, String> externalResponse) {
                return new TicketResult(
                        externalResponse.get("ticketId"), externalResponse.get("status"));
            }
        };
    }

    public record CreateTicketRequest(String subject, String description, String priority) {}

    public record TicketResult(String ticketId, String status) {}
}
