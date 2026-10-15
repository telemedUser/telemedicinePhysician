package com.telemedicine.common.integration.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.integration.ProviderCategory;
import com.telemedicine.common.integration.ProviderRegistry;
import com.telemedicine.common.integration.config.PlatformConfigSnapshot;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProviderSelectorBranchTest {

    @Test
    void fallsBackWhenAllProvidersUnhealthy() {
        ProviderRegistry<String> registry = new ProviderRegistry<>();
        registry.register("simulator", "sim");
        registry.register("vonage", "von");

        StubConfigClient client =
                new StubConfigClient(
                        new PlatformConfigSnapshot(
                                "vonage",
                                "simulator",
                                true,
                                Map.of(),
                                Map.of(),
                                "{}",
                                Instant.now()));
        ProviderSelector<String> selector =
                new ProviderSelector<>(
                        registry, client, ProviderCategory.VIDEO, "simulator", name -> false);

        assertThat(selector.active()).isEqualTo("sim");
    }

    @Test
    void selectsAlternateWhenSafeDefaultAlsoUnhealthy() {
        ProviderRegistry<String> registry = new ProviderRegistry<>();
        registry.register("simulator", "sim");
        registry.register("vonage", "von");
        registry.register("acs", "acs");

        StubConfigClient client =
                new StubConfigClient(
                        new PlatformConfigSnapshot(
                                "vonage",
                                "simulator",
                                true,
                                Map.of(),
                                Map.of(),
                                "{}",
                                Instant.now()));
        ProviderSelector<String> selector =
                new ProviderSelector<>(
                        registry,
                        client,
                        ProviderCategory.VIDEO,
                        "simulator",
                        name -> "acs".equals(name));

        assertThat(selector.active()).isEqualTo("acs");
    }

    private static final class StubConfigClient
            extends com.telemedicine.common.integration.config.PlatformConfigClient {

        StubConfigClient(PlatformConfigSnapshot snapshot) {
            super(
                    org.springframework.web.client.RestClient.builder()
                            .baseUrl("http://localhost")
                            .build(),
                    new com.fasterxml.jackson.databind.ObjectMapper(),
                    java.time.Duration.ofMinutes(1));
            this.snapshot = snapshot;
        }

        private final PlatformConfigSnapshot snapshot;

        @Override
        public PlatformConfigSnapshot snapshot() {
            return snapshot;
        }
    }
}
