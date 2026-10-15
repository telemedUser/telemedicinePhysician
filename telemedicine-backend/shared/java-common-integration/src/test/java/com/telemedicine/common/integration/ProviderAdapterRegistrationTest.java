package com.telemedicine.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.integration.autoconfigure.IntegrationAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProviderAdapterRegistrationTest {

    @ProviderAdapter(value = "vonage", category = ProviderCategory.VIDEO)
    static class VonageStub {}

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(IntegrationAutoConfiguration.class))
                    .withUserConfiguration(VonageStub.class);

    @Test
    void registersProviderAdapterBeans() {
        runner.run(
                context -> {
                    @SuppressWarnings("unchecked")
                    ProviderRegistry<Object> registry =
                            context.getBean("videoProviderRegistry", ProviderRegistry.class);
                    assertThat(registry.listProviders()).contains("vonage");
                });
    }
}
