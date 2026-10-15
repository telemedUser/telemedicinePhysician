package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

class MdcClientHttpRequestInterceptorBranchTest {

    @Test
    void doesNotOverrideExistingHeaderOrBlankMdc() throws Exception {
        MdcClientHttpRequestInterceptor interceptor = new MdcClientHttpRequestInterceptor();
        MockClientHttpRequest request =
                new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/test"));
        request.getHeaders().set(Headers.CORRELATION_ID, "existing");

        ClientHttpRequestExecution execution =
                (req, body) ->
                        new MockClientHttpResponse(
                                new byte[0], org.springframework.http.HttpStatus.OK);

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(Headers.CORRELATION_ID)).isEqualTo("existing");
    }
}
