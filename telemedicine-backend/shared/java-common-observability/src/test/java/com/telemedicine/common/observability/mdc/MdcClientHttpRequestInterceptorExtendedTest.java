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
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

class MdcClientHttpRequestInterceptorExtendedTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void forwardsOptionalHeadersWhenPresent() throws Exception {
        MDC.put(MdcKeys.CAUSATION_ID, "cause-x");
        MDC.put(MdcKeys.TENANT_ID, "tenant-x");
        MDC.put(MdcKeys.REQUEST_ID, "req-x");
        MdcClientHttpRequestInterceptor interceptor = new MdcClientHttpRequestInterceptor();
        MockClientHttpRequest request =
                new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/test"));
        ClientHttpRequestExecution execution =
                (req, body) ->
                        new MockClientHttpResponse(
                                new byte[0], org.springframework.http.HttpStatus.OK);

        interceptor.intercept(request, new byte[0], execution).close();

        assertThat(request.getHeaders().getFirst(Headers.CAUSATION_ID)).isEqualTo("cause-x");
        assertThat(request.getHeaders().getFirst(Headers.TENANT_ID)).isEqualTo("tenant-x");
        assertThat(request.getHeaders().getFirst(Headers.REQUEST_ID)).isEqualTo("req-x");
    }
}
