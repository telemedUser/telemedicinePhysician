package com.telemedicine.common.integration.resilience;

import java.util.function.Predicate;

/**
 * Resilience4j retry predicate that refuses to retry non-idempotent POST calls without an {@code
 * X-Idempotency-Key}.
 */
public class IdempotencyAwareRetryPredicate implements Predicate<Throwable> {

    private final Predicate<Throwable> delegate;

    public IdempotencyAwareRetryPredicate(Predicate<Throwable> delegate) {
        this.delegate = delegate == null ? throwable -> true : delegate;
    }

    @Override
    public boolean test(Throwable throwable) {
        if (!delegate.test(throwable)) {
            return false;
        }
        return IntegrationCallContext.currentCallIsRetrySafe();
    }
}
