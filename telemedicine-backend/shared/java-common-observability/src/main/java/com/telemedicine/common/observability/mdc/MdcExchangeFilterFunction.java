package com.telemedicine.common.observability.mdc;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/** Forwards correlation headers from MDC on outbound {@code WebClient} calls. */
public final class MdcExchangeFilterFunction {

    private MdcExchangeFilterFunction() {}

    public static ExchangeFilterFunction create() {
        return (request, next) -> {
            MdcContext context = MdcContext.capture();
            context.apply();
            ClientRequest mutated =
                    ClientRequest.from(request)
                            .headers(
                                    headers -> {
                                        forward(
                                                headers,
                                                Headers.CORRELATION_ID,
                                                MDC.get(MdcKeys.CORRELATION_ID));
                                        forward(
                                                headers,
                                                Headers.CAUSATION_ID,
                                                MDC.get(MdcKeys.CAUSATION_ID));
                                        forward(
                                                headers,
                                                Headers.TENANT_ID,
                                                MDC.get(MdcKeys.TENANT_ID));
                                        forward(
                                                headers,
                                                Headers.REQUEST_ID,
                                                MDC.get(MdcKeys.REQUEST_ID));
                                    })
                            .build();
            return next.exchange(mutated).doFinally(signal -> context.apply());
        };
    }

    private static void forward(HttpHeaders headers, String headerName, String value) {
        if (value != null && !value.isBlank() && !headers.containsKey(headerName)) {
            headers.set(headerName, value);
        }
    }
}
