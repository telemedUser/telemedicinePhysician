package com.telemedicine.common.web.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.telemedicine.common.web.TelemedicineWebProperties;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

    private final TelemedicineWebProperties.RequestLogging properties =
            new TelemedicineWebProperties().getRequestLogging();
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void captureLogs() {
        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(appender);
    }

    @Test
    void logsSuccessfulRequestsAtInfo() throws Exception {
        run(new RequestLoggingFilter(properties), request("GET", "/v1/appointments", null), 200);

        assertThat(events())
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getLevel()).isEqualTo(Level.INFO);
                            assertThat(event.getFormattedMessage())
                                    .contains("GET", "/v1/appointments", "200");
                        });
    }

    @Test
    void logsServerErrorsAtError() throws Exception {
        run(new RequestLoggingFilter(properties), request("POST", "/v1/payments", null), 500);

        assertThat(events())
                .singleElement()
                .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.ERROR));
    }

    @Test
    void logsClientErrorsAtWarn() throws Exception {
        run(new RequestLoggingFilter(properties), request("POST", "/v1/payments", null), 400);

        assertThat(events())
                .singleElement()
                .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
    }

    @Test
    @DisplayName("a query string carrying a RUT is redacted before it reaches the log")
    void redactsTheQueryString() throws Exception {
        run(
                new RequestLoggingFilter(properties),
                request("GET", "/v1/patients", "rut=12.345.678-5"),
                200);

        assertThat(events())
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getFormattedMessage()).contains("[RUT[REDACTED]]");
                            assertThat(event.getFormattedMessage()).doesNotContain("12.345.678");
                        });
    }

    @Test
    @DisplayName("a successful request is dropped when sampled out, but a failure never is")
    void samplingSuppressesOnlySuccesses() throws Exception {
        properties.setSampleRate(0.0);
        RequestLoggingFilter filter = new RequestLoggingFilter(properties);

        run(filter, request("GET", "/v1/appointments", null), 200);
        assertThat(events()).isEmpty();

        run(filter, request("GET", "/v1/appointments", null), 503);
        assertThat(events()).hasSize(1);
    }

    @Test
    void samplingAtFullRateAlwaysLogs() throws Exception {
        properties.setSampleRate(1.0);

        run(new RequestLoggingFilter(properties), request("GET", "/v1/x", null), 200);

        assertThat(events()).hasSize(1);
    }

    @Test
    void partialSamplingIsExercised() throws Exception {
        properties.setSampleRate(0.5);
        RequestLoggingFilter filter = new RequestLoggingFilter(properties);

        for (int i = 0; i < 50; i++) {
            run(filter, request("GET", "/v1/x", null), 200);
        }

        assertThat(events().size()).isBetween(0, 50);
    }

    @Test
    @DisplayName("a request slower than the SLO threshold is logged even when sampled out")
    void slowRequestsAlwaysLogAtWarn() throws Exception {
        properties.setSampleRate(0.0);
        properties.setSlowRequestThreshold(Duration.ZERO);

        run(new RequestLoggingFilter(properties), request("GET", "/v1/slow", null), 200);

        assertThat(events())
                .singleElement()
                .satisfies(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
    }

    @Test
    void actuatorTrafficIsExcluded() {
        RequestLoggingFilter filter = new RequestLoggingFilter(properties);

        assertThat(filter.shouldNotFilter(request("GET", "/actuator/health", null))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/favicon.ico", null))).isTrue();
        assertThat(filter.shouldNotFilter(request("GET", "/v1/appointments", null))).isFalse();
    }

    private List<ILoggingEvent> events() {
        return List.copyOf(appender.list);
    }

    private static void run(RequestLoggingFilter filter, MockHttpServletRequest request, int status)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        FilterChain chain = new MockFilterChain();
        filter.doFilterInternal(request, response, chain);
    }

    private static MockHttpServletRequest request(String method, String uri, String query) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setQueryString(query);
        return request;
    }
}
