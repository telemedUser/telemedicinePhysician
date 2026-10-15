package com.telemedicine.common.integration.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

class SimScenarioExchangeFilterFunctionTest {

    @AfterEach
    void tearDown() {
        MDC.remove(Headers.SIM_SCENARIO);
    }

    @Test
    void forwardsScenarioHeaderOnWebClient() {
        ExchangeFunction exchange =
                request -> {
                    assertThat(request.headers().getFirst(Headers.SIM_SCENARIO))
                            .isEqualTo("failure");
                    return Mono.empty();
                };

        MDC.put(Headers.SIM_SCENARIO, "failure");
        SimScenarioExchangeFilterFunction.create()
                .filter(
                        ClientRequest.create(
                                        org.springframework.http.HttpMethod.GET,
                                        java.net.URI.create("http://localhost"))
                                .build(),
                        exchange)
                .block();
    }
}
