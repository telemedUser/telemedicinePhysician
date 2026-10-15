package com.telemedicine.common.integration.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.integration.ProviderCategory;
import com.telemedicine.common.integration.ProviderRegistry;
import com.telemedicine.common.integration.config.PlatformConfigSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProviderSelectorExtendedTest {

    @Test
    void notifiesListenersOnChange() {
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
                        registry, client, ProviderCategory.VIDEO, "simulator", name -> true);
        List<String> changes = new ArrayList<>();
        selector.addListener((category, previous, current) -> changes.add(current));

        selector.active();
        client.setVideoProvider("simulator");
        selector.active();

        assertThat(changes).containsExactly("vonage", "simulator");
        assertThat(selector.category()).isEqualTo(ProviderCategory.VIDEO);
    }

    @Test
    void resolvesChatCategoryFromSnapshot() {
        ProviderRegistry<String> registry = new ProviderRegistry<>();
        registry.register("simulator", "sim");
        registry.register("zendesk", "zd");

        StubConfigClient client =
                new StubConfigClient(
                        new PlatformConfigSnapshot(
                                "simulator",
                                "zendesk",
                                true,
                                Map.of(),
                                Map.of(),
                                "{}",
                                Instant.now()));
        ProviderSelector<String> selector =
                new ProviderSelector<>(
                        registry, client, ProviderCategory.CHAT, "simulator", name -> true);

        assertThat(selector.configuredProviderName()).isEqualTo("zendesk");
        assertThat(selector.active()).isEqualTo("zd");
    }

    private static final class StubConfigClient
            extends com.telemedicine.common.integration.config.PlatformConfigClient {

        private PlatformConfigSnapshot snapshot;

        StubConfigClient(PlatformConfigSnapshot snapshot) {
            super(
                    org.springframework.web.client.RestClient.builder()
                            .baseUrl("http://localhost")
                            .build(),
                    new com.fasterxml.jackson.databind.ObjectMapper(),
                    java.time.Duration.ofMinutes(1));
            this.snapshot = snapshot;
        }

        void setVideoProvider(String provider) {
            snapshot =
                    new PlatformConfigSnapshot(
                            provider,
                            snapshot.chatProvider(),
                            snapshot.telemetryEnabled(),
                            snapshot.notificationChannels(),
                            snapshot.mfeRemotes(),
                            snapshot.telemetryRulesJson(),
                            snapshot.refreshedAt());
        }

        @Override
        public PlatformConfigSnapshot snapshot() {
            return snapshot;
        }
    }
}
