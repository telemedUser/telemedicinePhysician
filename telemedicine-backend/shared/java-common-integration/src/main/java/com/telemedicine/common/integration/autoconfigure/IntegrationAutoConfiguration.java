package com.telemedicine.common.integration.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.events.EventPublisher;
import com.telemedicine.common.integration.ProviderAdapter;
import com.telemedicine.common.integration.ProviderCategory;
import com.telemedicine.common.integration.ProviderRegistry;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.integration.provider.ProviderHealthChecker;
import com.telemedicine.common.integration.provider.ProviderSelector;
import com.telemedicine.common.integration.webhook.WebhookSignatureVerifier;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(prefix = "telemedicine.integration", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TelemedicineIntegrationProperties.class)
public class IntegrationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IntegrationHttpClientFactory integrationHttpClientFactory(
            TelemedicineIntegrationProperties properties,
            ObjectProvider<List<ClientHttpRequestInterceptor>> restInterceptors,
            ObjectProvider<List<ExchangeFilterFunction>> webFilters,
            ObjectProvider<ObservationRegistry> observationRegistry) {
        return new IntegrationHttpClientFactory(
                properties,
                restInterceptors.getIfAvailable(ArrayList::new),
                webFilters.getIfAvailable(ArrayList::new),
                observationRegistry.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformConfigClient platformConfigClient(
            IntegrationHttpClientFactory clientFactory,
            ObjectProvider<ObjectMapper> objectMapper,
            TelemedicineIntegrationProperties properties) {
        RestClient restClient = clientFactory.restClient(properties.getConfigServiceBaseUrl());
        ObjectMapper mapper = objectMapper.getIfAvailable(ObjectMapper::new);
        return new PlatformConfigClient(restClient, mapper, properties.getConfigCacheTtl());
    }

    @Bean
    @ConditionalOnMissingBean
    public IntegrationFailureReporter integrationFailureReporter(
            ObjectProvider<EventPublisher> eventPublisher) {
        EventPublisher publisher = eventPublisher.getIfAvailable();
        Optional<IntegrationFailureReporter.IntegrationEventPublisher> adapter =
                publisher == null
                        ? Optional.empty()
                        : Optional.of((subject, payload) -> publisher.publish(subject, payload));
        return new IntegrationFailureReporter(adapter);
    }

    @Bean
    @ConditionalOnMissingBean(name = "videoProviderRegistry")
    public ProviderRegistry<Object> videoProviderRegistry() {
        return new ProviderRegistry<>();
    }

    @Bean
    @ConditionalOnMissingBean(name = "chatProviderRegistry")
    public ProviderRegistry<Object> chatProviderRegistry() {
        return new ProviderRegistry<>();
    }

    @Bean
    @ConditionalOnMissingBean(name = "videoProviderSelector")
    public ProviderSelector<Object> videoProviderSelector(
            ProviderRegistry<Object> videoProviderRegistry,
            PlatformConfigClient platformConfigClient,
            TelemedicineIntegrationProperties properties,
            ObjectProvider<ProviderHealthChecker> healthChecker) {
        return new ProviderSelector<>(
                videoProviderRegistry,
                platformConfigClient,
                ProviderCategory.VIDEO,
                properties.getSafeDefaultProvider(),
                healthChecker.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(name = "chatProviderSelector")
    public ProviderSelector<Object> chatProviderSelector(
            ProviderRegistry<Object> chatProviderRegistry,
            PlatformConfigClient platformConfigClient,
            TelemedicineIntegrationProperties properties,
            ObjectProvider<ProviderHealthChecker> healthChecker) {
        return new ProviderSelector<>(
                chatProviderRegistry,
                platformConfigClient,
                ProviderCategory.CHAT,
                properties.getSafeDefaultProvider(),
                healthChecker.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telemedicine.integration.webhook", name = "secret")
    public WebhookSignatureVerifier webhookSignatureVerifier(
            TelemedicineIntegrationProperties properties,
            @org.springframework.beans.factory.annotation.Value(
                            "${telemedicine.integration.webhook.secret}")
                    String secret) {
        return new WebhookSignatureVerifier(secret, properties.getWebhook());
    }

    @Bean
    static ProviderAdapterRegistrar providerAdapterRegistrar(
            org.springframework.beans.factory.ListableBeanFactory beanFactory) {
        return new ProviderAdapterRegistrar(beanFactory);
    }

    @Configuration
    @ConditionalOnClass(
            name = "com.telemedicine.common.observability.mdc.MdcClientHttpRequestInterceptor")
    static class ObservabilityPropagationConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "telemedicineIntegrationRestInterceptors")
        List<ClientHttpRequestInterceptor> telemedicineIntegrationRestInterceptors() {
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
            interceptors.add(
                    new com.telemedicine.common.observability.mdc
                            .MdcClientHttpRequestInterceptor());
            return interceptors;
        }

        @Bean
        @ConditionalOnMissingBean(name = "telemedicineIntegrationWebFilters")
        List<ExchangeFilterFunction> telemedicineIntegrationWebFilters() {
            List<ExchangeFilterFunction> filters = new ArrayList<>();
            filters.add(
                    com.telemedicine.common.observability.mdc.MdcExchangeFilterFunction.create());
            return filters;
        }
    }

    @Configuration
    @ConditionalOnClass(
            name = "com.telemedicine.common.security.propagation.ClaimPropagationInterceptor")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
            com.telemedicine.common.security.access.SecurityContextAccessor.class)
    static class SecurityPropagationConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "telemedicineIntegrationAuthInterceptor")
        ClientHttpRequestInterceptor telemedicineIntegrationAuthInterceptor(
                com.telemedicine.common.security.access.SecurityContextAccessor securityContext,
                com.telemedicine.common.security.TelemedicineSecurityProperties properties) {
            return new com.telemedicine.common.security.propagation.ClaimPropagationInterceptor(
                    securityContext, properties.getPropagation());
        }

        @Bean
        @ConditionalOnMissingBean(name = "telemedicineIntegrationAuthWebFilter")
        ExchangeFilterFunction telemedicineIntegrationAuthWebFilter(
                com.telemedicine.common.security.access.SecurityContextAccessor securityContext,
                com.telemedicine.common.security.TelemedicineSecurityProperties properties) {
            return new com.telemedicine.common.security.propagation.ClaimPropagationExchangeFilter(
                    securityContext, properties.getPropagation());
        }
    }

    static final class ProviderAdapterRegistrar implements BeanPostProcessor {

        private final org.springframework.beans.factory.ListableBeanFactory beanFactory;

        ProviderAdapterRegistrar(
                org.springframework.beans.factory.ListableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName)
                throws BeansException {
            Class<?> type = org.springframework.util.ClassUtils.getUserClass(bean);
            ProviderAdapter annotation = type.getAnnotation(ProviderAdapter.class);
            if (annotation == null) {
                return bean;
            }
            ProviderRegistry<Object> registry = registryFor(annotation.category());
            registry.register(annotation.value(), bean);
            return bean;
        }

        @SuppressWarnings("unchecked")
        private ProviderRegistry<Object> registryFor(ProviderCategory category) {
            String beanName =
                    category == ProviderCategory.VIDEO
                            ? "videoProviderRegistry"
                            : "chatProviderRegistry";
            return (ProviderRegistry<Object>) beanFactory.getBean(beanName);
        }
    }
}
