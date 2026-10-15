package com.telemedicine.common.integration.client;

import com.telemedicine.common.core.constants.Headers;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Forwards {@link Headers#SIM_SCENARIO} from MDC on outbound RestClient calls. */
public class SimScenarioPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String scenario = MDC.get(Headers.SIM_SCENARIO);
        if (scenario != null
                && !scenario.isBlank()
                && !request.getHeaders().containsKey(Headers.SIM_SCENARIO)) {
            request.getHeaders().set(Headers.SIM_SCENARIO, scenario);
        }
        return execution.execute(request, body);
    }
}
