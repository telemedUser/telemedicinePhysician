package com.telemedicine.common.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class IntegrationAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(IntegrationAutoConfiguration.class))
                    .withPropertyValues(
                            "telemedicine.integration.config-service-base-url=http://localhost:8112",
                            "spring.application.name=identity-service");

    @Test
    void registersIntegrationBeans() {
        runner.run(
                context ->
                        assertThat(context)
                                .hasSingleBean(IntegrationHttpClientFactory.class)
                                .hasSingleBean(PlatformConfigClient.class)
                                .hasSingleBean(IntegrationFailureReporter.class)
                                .hasBean("videoProviderRegistry")
                                .hasBean("chatProviderRegistry")
                                .hasBean("videoProviderSelector")
                                .hasBean("chatProviderSelector"));
    }

    @Test
    void bindsProperties() {
        runner.withPropertyValues("telemedicine.integration.simulator.base-url=http://sim:8200")
                .run(
                        context ->
                                assertThat(
                                                context.getBean(
                                                                TelemedicineIntegrationProperties
                                                                        .class)
                                                        .getSimulator()
                                                        .getBaseUrl())
                                        .isEqualTo("http://sim:8200"));
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("telemedicine.integration.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(IntegrationHttpClientFactory.class)
                                        .doesNotHaveBean(PlatformConfigClient.class));
    }

    @Test
    void createsWebhookVerifierWhenSecretConfigured() {
        runner.withPropertyValues("telemedicine.integration.webhook.secret=abc")
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(
                                                com.telemedicine.common.integration.webhook
                                                        .WebhookSignatureVerifier.class));
    }
}
