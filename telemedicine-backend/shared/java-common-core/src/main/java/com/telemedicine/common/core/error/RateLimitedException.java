package com.telemedicine.common.core.error;

import java.time.Duration;
import java.util.Map;

/** The caller exceeded a rate limit or quota. Carries the delay to put in {@code Retry-After}. */
public class RateLimitedException extends DomainException {

    private final Duration retryAfter;

    public RateLimitedException(Duration retryAfter) {
        super(
                ErrorCode.RATE_LIMITED,
                "Rate limit exceeded",
                Map.of("retryAfterSeconds", retryAfter == null ? 0L : retryAfter.toSeconds()));
        this.retryAfter = retryAfter == null ? Duration.ZERO : retryAfter;
    }

    /** Never null; {@link Duration#ZERO} when the limiter did not supply a hint. */
    public Duration getRetryAfter() {
        return retryAfter;
    }
}
