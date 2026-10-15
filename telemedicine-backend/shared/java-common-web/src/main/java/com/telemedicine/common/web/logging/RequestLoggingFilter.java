package com.telemedicine.common.web.logging;

import com.telemedicine.common.core.phi.PhiRedactor;
import com.telemedicine.common.web.TelemedicineWebProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Access log for the shared web layer.
 *
 * <p>Sampled rather than exhaustive: the platform's latency budget is 300 ms and the video and
 * waiting-room endpoints are polled continuously, so logging every request costs measurable time
 * and floods Elasticsearch with lines nobody reads. Anything that fails or runs slow is always
 * logged regardless of the sample rate — those are the lines that matter during an incident.
 *
 * <p>Query strings routinely carry RUTs and e-mail addresses, so the URI is redacted before it is
 * written.
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final TelemedicineWebProperties.RequestLogging properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final long slowThresholdMillis;

    public RequestLoggingFilter(TelemedicineWebProperties.RequestLogging properties) {
        this.properties = properties;
        this.slowThresholdMillis = properties.getSlowRequestThreshold().toMillis();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        List<String> excluded = properties.getExcludePatterns();
        String path = request.getRequestURI();
        return excluded.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long startedAt = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            int status = response.getStatus();
            boolean slow = elapsedMillis >= slowThresholdMillis;

            if (status >= 500) {
                log.error(
                        "{} {} -> {} in {}ms",
                        request.getMethod(),
                        path(request),
                        status,
                        elapsedMillis);
            } else if (status >= 400 || slow) {
                log.warn(
                        "{} {} -> {} in {}ms",
                        request.getMethod(),
                        path(request),
                        status,
                        elapsedMillis);
            } else if (shouldSample()) {
                log.info(
                        "{} {} -> {} in {}ms",
                        request.getMethod(),
                        path(request),
                        status,
                        elapsedMillis);
            }
        }
    }

    private boolean shouldSample() {
        double rate = properties.getSampleRate();
        if (rate >= 1.0) {
            return true;
        }
        if (rate <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < rate;
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return PhiRedactor.redact(query == null ? uri : uri + "?" + query);
    }
}
