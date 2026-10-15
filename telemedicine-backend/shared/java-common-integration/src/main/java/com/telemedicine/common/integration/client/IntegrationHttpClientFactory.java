package com.telemedicine.common.integration.client;

import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builds {@link RestClient} and {@link WebClient} instances with shared timeouts, correlation/auth
 * propagation, OTel observation and PHI-redacted logging.
 */
public class IntegrationHttpClientFactory {

    private final TelemedicineIntegrationProperties properties;
    private final List<ClientHttpRequestInterceptor> restInterceptors;
    private final List<org.springframework.web.reactive.function.client.ExchangeFilterFunction>
            webFilters;
    private final ObservationRegistry observationRegistry;

    public IntegrationHttpClientFactory(
            TelemedicineIntegrationProperties properties,
            List<ClientHttpRequestInterceptor> restInterceptors,
            List<org.springframework.web.reactive.function.client.ExchangeFilterFunction>
                    webFilters,
            ObservationRegistry observationRegistry) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.restInterceptors =
                restInterceptors == null ? List.of() : List.copyOf(restInterceptors);
        this.webFilters = webFilters == null ? List.of() : List.copyOf(webFilters);
        this.observationRegistry = observationRegistry;
    }

    public RestClient.Builder restClientBuilder() {
        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory());
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new SimScenarioPropagationInterceptor());
        interceptors.add(new IntegrationLoggingInterceptor(properties.getHttp().isLogRequests()));
        interceptors.addAll(restInterceptors);
        builder.requestInterceptors(list -> list.addAll(interceptors));
        if (observationRegistry != null) {
            builder.observationRegistry(observationRegistry);
        }
        return builder;
    }

    public RestClient restClient(String baseUrl) {
        return restClientBuilder().baseUrl(baseUrl).build();
    }

    public WebClient.Builder webClientBuilder() {
        WebClient.Builder builder = WebClient.builder();
        webFilters.forEach(builder::filter);
        builder.filter(SimScenarioExchangeFilterFunction.create());
        if (observationRegistry != null) {
            builder.observationRegistry(observationRegistry);
        }
        return builder;
    }

    public WebClient webClient(String baseUrl) {
        return webClientBuilder().baseUrl(baseUrl).build();
    }

    public ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        TelemedicineIntegrationProperties.Http http = properties.getHttp();
        factory.setConnectTimeout(toMillis(http.getConnectTimeout()));
        factory.setReadTimeout(toMillis(http.getReadTimeout()));
        return factory;
    }

    /** Applies additional customisation without rebuilding the shared interceptors. */
    public RestClient restClient(String baseUrl, Consumer<RestClient.Builder> customizer) {
        RestClient.Builder builder = restClientBuilder().baseUrl(baseUrl);
        if (customizer != null) {
            customizer.accept(builder);
        }
        return builder.build();
    }

    private static int toMillis(Duration duration) {
        Duration value = duration == null ? Duration.ofSeconds(30) : duration;
        long millis = value.toMillis();
        if (millis > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) millis;
    }
}
