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
public class FonasaAdapter
        extends ExternalSystemAdapter<
                FonasaAdapter.ChallengeRequest, Map<String, String>, Map<String, String>, FonasaAdapter.ChallengeResult> {

    private final RestClient client;
    private final IntegrationFailureReporter failureReporter;

    public FonasaAdapter(
            IntegrationHttpClientFactory clientFactory,
            IntegrationFailureReporter failureReporter,
            TelemedicineIntegrationProperties properties) {
        super(ExternalSystems.FONASA, failureReporter, FonasaAdapter.challengeMapper());
        this.failureReporter = failureReporter;
        this.client =
                clientFactory.restClient(
                        AdapterSupport.trimTrailingSlash(properties.getSimulator().getBaseUrl())
                                + "/sim/fonasa");
    }

    public ChallengeResult challenge(String patientId) {
        return execute(new ChallengeRequest(patientId), "2faChallenge", patientId);
    }

    public VerifyResult verify(String patientId, String challengeId, String code) {
        return AdapterSupport.execute(
                failureReporter,
                ExternalSystems.FONASA,
                "2faVerify",
                patientId,
                () -> {
                    Map<String, Object> response =
                            client.post()
                                    .uri("/2fa/verify")
                                    .body(
                                            Map.of(
                                                    "patientId", patientId,
                                                    "challengeId", challengeId,
                                                    "code", code))
                                    .retrieve()
                                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                    Object verified = response.get("verified");
                    boolean isVerified =
                            verified instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(verified));
                    return new VerifyResult(isVerified, String.valueOf(response.get("patientId")));
                });
    }

    @Override
    protected Map<String, String> invoke(Map<String, String> externalRequest) {
        return client.post()
                .uri("/2fa/challenge")
                .body(externalRequest)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, String>>() {});
    }

    private static AntiCorruptionMapper<
                    ChallengeRequest, Map<String, String>, Map<String, String>, ChallengeResult>
            challengeMapper() {
        return new AntiCorruptionMapper<>() {
            @Override
            public Map<String, String> toExternal(ChallengeRequest domainRequest) {
                return Map.of("patientId", domainRequest.patientId());
            }

            @Override
            public ChallengeResult toDomain(Map<String, String> externalResponse) {
                return new ChallengeResult(
                        externalResponse.get("challengeId"), externalResponse.get("maskedPhone"));
            }
        };
    }

    public record ChallengeRequest(String patientId) {}

    public record ChallengeResult(String challengeId, String maskedPhone) {}

    public record VerifyResult(boolean verified, String patientId) {}
}
