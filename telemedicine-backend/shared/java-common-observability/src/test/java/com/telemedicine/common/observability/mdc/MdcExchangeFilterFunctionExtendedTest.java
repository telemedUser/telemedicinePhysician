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

class MdcExchangeFilterFunctionExtendedTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void forwardsOptionalHeadersAndSkipsExisting() {
        MDC.put(MdcKeys.CAUSATION_ID, "cause-w");
        MDC.put(MdcKeys.TENANT_ID, "tenant-w");
        ExchangeFunction exchangeFunction =
                request -> {
                    assertThat(request.headers().getFirst(Headers.CAUSATION_ID))
                            .isEqualTo("pre-set");
                    assertThat(request.headers().getFirst(Headers.TENANT_ID)).isEqualTo("tenant-w");
                    return Mono.empty();
                };

        MdcExchangeFilterFunction.create()
                .filter(
                        ClientRequest.create(
                                        org.springframework.http.HttpMethod.GET,
                                        java.net.URI.create("http://localhost"))
                                .header(Headers.CAUSATION_ID, "pre-set")
                                .build(),
                        exchangeFunction)
                .block();
    }
}
