package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

class MdcExchangeFilterFunctionTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void forwardsCorrelationOnWebClientExchange() {
        MDC.put(MdcKeys.CORRELATION_ID, "web-corr");
        ExchangeFunction exchangeFunction =
                request -> {
                    assertThat(request.headers().getFirst(Headers.CORRELATION_ID))
                            .isEqualTo("web-corr");
                    return Mono.empty();
                };

        MdcExchangeFilterFunction.create()
                .filter(
                        ClientRequest.create(
                                        org.springframework.http.HttpMethod.GET,
                                        java.net.URI.create("http://localhost"))
                                .build(),
                        exchangeFunction)
                .block();

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isEqualTo("web-corr");
    }
}
