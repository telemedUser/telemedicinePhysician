package com.telemedicine.common.integration.client;

import com.telemedicine.common.core.constants.Headers;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/** Forwards {@link Headers#SIM_SCENARIO} from MDC on outbound WebClient calls. */
public final class SimScenarioExchangeFilterFunction {

    private SimScenarioExchangeFilterFunction() {}

    public static ExchangeFilterFunction create() {
        return (request, next) -> {
            String scenario = MDC.get(Headers.SIM_SCENARIO);
            if (scenario == null
                    || scenario.isBlank()
                    || request.headers().containsKey(Headers.SIM_SCENARIO)) {
                return next.exchange(request);
            }
            ClientRequest mutated =
                    ClientRequest.from(request)
                            .headers(headers -> headers.set(Headers.SIM_SCENARIO, scenario))
                            .build();
            return next.exchange(mutated);
        };
    }
}
