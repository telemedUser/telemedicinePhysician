package com.telemedicine.common.observability.autoconfigure;

import com.telemedicine.common.observability.TelemedicineObservabilityProperties;
import com.telemedicine.common.observability.filter.CorrelationIdFilter;
import com.telemedicine.common.observability.mdc.MdcClientHttpRequestInterceptor;
import com.telemedicine.common.observability.mdc.MdcExchangeFilterFunction;
import com.telemedicine.common.observability.mdc.MdcKafkaConsumerInterceptor;
import com.telemedicine.common.observability.mdc.MdcKafkaProducerInterceptor;
import com.telemedicine.common.observability.mdc.MdcPropagatingTaskDecorator;
import com.telemedicine.common.observability.mdc.MdcSchedulingAspect;
import com.telemedicine.common.observability.metrics.JourneyMetrics;
import com.telemedicine.common.observability.metrics.ObservabilityMeterRegistryCustomizer;
import com.telemedicine.common.observability.tracing.ObservabilityObservationFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

/** Registers correlation, MDC propagation, metrics, tracing and logging defaults. */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "telemedicine.observability",
        name = "enabled",
        matchIfMissing = true)
@EnableConfigurationProperties(TelemedicineObservabilityProperties.class)
public class ObservabilityAutoConfiguration {

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ServletCorrelationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public CorrelationIdFilter correlationIdFilter(
                TelemedicineObservabilityProperties properties, ObjectProvider<Tracer> tracer) {
            return new CorrelationIdFilter(properties, tracer.getIfAvailable());
        }

        @Bean
        @ConditionalOnMissingBean(name = "correlationIdFilterRegistration")
        public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(
                CorrelationIdFilter filter) {
            FilterRegistrationBean<CorrelationIdFilter> registration =
                    new FilterRegistrationBean<>();
            registration.setFilter(filter);
            registration.setOrder(Integer.MIN_VALUE);
            registration.addUrlPatterns("/*");
            return registration;
        }
    }

    @Configuration
    @EnableScheduling
    static class SchedulingMdcConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public MdcSchedulingAspect mdcSchedulingAspect() {
            return new MdcSchedulingAspect();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskDecorator mdcPropagatingTaskDecorator() {
        return new MdcPropagatingTaskDecorator();
    }

    @Bean
    @ConditionalOnMissingBean
    public MeterRegistryCustomizer<MeterRegistry> observabilityMeterRegistryCustomizer(
            org.springframework.core.env.Environment environment,
            TelemedicineObservabilityProperties properties) {
        return new ObservabilityMeterRegistryCustomizer(environment, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JourneyMetrics journeyMetrics(io.micrometer.core.instrument.MeterRegistry registry) {
        return new JourneyMetrics(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObservabilityObservationFilter observabilityObservationFilter() {
        return new ObservabilityObservationFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public MdcClientHttpRequestInterceptor mdcClientHttpRequestInterceptor() {
        return new MdcClientHttpRequestInterceptor();
    }

    @Configuration
    @ConditionalOnClass(WebClient.class)
    static class WebClientMdcConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "mdcWebClientCustomizer")
        public WebClientCustomizer mdcWebClientCustomizer() {
            return builder -> builder.filter(MdcExchangeFilterFunction.create());
        }
    }

    @Configuration
    @ConditionalOnClass(name = "org.apache.kafka.clients.producer.ProducerInterceptor")
    static class KafkaMdcConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public MdcKafkaProducerInterceptor mdcKafkaProducerInterceptor() {
            return new MdcKafkaProducerInterceptor();
        }

        @Bean
        @ConditionalOnMissingBean
        public MdcKafkaConsumerInterceptor mdcKafkaConsumerInterceptor() {
            return new MdcKafkaConsumerInterceptor();
        }

        @Bean
        @ConditionalOnMissingBean(name = "mdcKafkaRecordInterceptor")
        public RecordInterceptor<Object, Object> mdcKafkaRecordInterceptor() {
            return (record, consumer) -> {
                MdcKafkaConsumerInterceptor.applyRecordContext(record);
                return record;
            };
        }
    }
}
