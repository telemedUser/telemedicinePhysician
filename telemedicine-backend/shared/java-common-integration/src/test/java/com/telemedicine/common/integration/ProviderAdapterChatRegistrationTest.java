package com.telemedicine.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.integration.autoconfigure.IntegrationAutoConfiguration;
import com.telemedicine.common.integration.provider.ProviderSelector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProviderAdapterChatRegistrationTest {

    @ProviderAdapter(value = "zendesk", category = ProviderCategory.CHAT)
    static class ZendeskStub {}

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(IntegrationAutoConfiguration.class))
                    .withUserConfiguration(ZendeskStub.class);

    @Test
    void registersChatProviderAdapter() {
        runner.run(
                context -> {
                    @SuppressWarnings("unchecked")
                    ProviderRegistry<Object> registry =
                            context.getBean("chatProviderRegistry", ProviderRegistry.class);
                    assertThat(registry.listProviders()).contains("zendesk");
                    ProviderSelector<Object> selector =
                            context.getBean("chatProviderSelector", ProviderSelector.class);
                    assertThat(selector.category()).isEqualTo(ProviderCategory.CHAT);
                });
    }
}
