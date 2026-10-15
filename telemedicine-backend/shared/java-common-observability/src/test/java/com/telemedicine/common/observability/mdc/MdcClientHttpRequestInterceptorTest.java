package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.constants.MdcKeys;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

class MdcClientHttpRequestInterceptorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void forwardsCorrelationHeader() throws Exception {
        MDC.put(MdcKeys.CORRELATION_ID, "outbound-corr");
        MdcClientHttpRequestInterceptor interceptor = new MdcClientHttpRequestInterceptor();
        MockClientHttpRequest request =
                new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/test"));
        ClientHttpRequestExecution execution =
                (req, body) ->
                        new MockClientHttpResponse(
                                new byte[0], org.springframework.http.HttpStatus.OK);

        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(Headers.CORRELATION_ID))
                .isEqualTo("outbound-corr");
        response.close();
    }
}
