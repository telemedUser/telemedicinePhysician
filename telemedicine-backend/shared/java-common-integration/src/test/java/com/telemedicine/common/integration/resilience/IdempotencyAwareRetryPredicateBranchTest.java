package com.telemedicine.common.integration.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class IdempotencyAwareRetryPredicateBranchTest {

    @Test
    void delegatesFalseFromUnderlyingPredicate() {
        IdempotencyAwareRetryPredicate predicate = new IdempotencyAwareRetryPredicate(ex -> false);
        assertThat(predicate.test(new IOException())).isFalse();
    }

    @Test
    void blocksWhenNoContextSet() {
        IntegrationCallContext.clear();
        IdempotencyAwareRetryPredicate predicate = new IdempotencyAwareRetryPredicate(ex -> true);
        assertThat(predicate.test(new IOException())).isFalse();
    }
}
