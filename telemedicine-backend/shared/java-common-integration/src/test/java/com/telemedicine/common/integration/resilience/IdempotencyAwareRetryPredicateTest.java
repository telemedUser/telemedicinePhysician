package com.telemedicine.common.integration.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class IdempotencyAwareRetryPredicateTest {

    @AfterEach
    void tearDown() {
        IntegrationCallContext.clear();
    }

    @Test
    void allowsRetryForIdempotentGet() {
        IntegrationCallContext.set(IntegrationCallContext.forMethod(HttpMethod.GET, null));
        IdempotencyAwareRetryPredicate predicate = new IdempotencyAwareRetryPredicate(t -> true);

        assertThat(predicate.test(new IOException("timeout"))).isTrue();
    }

    @Test
    void blocksRetryForPostWithoutIdempotencyKey() {
        IntegrationCallContext.set(IntegrationCallContext.forMethod(HttpMethod.POST, null));
        IdempotencyAwareRetryPredicate predicate = new IdempotencyAwareRetryPredicate(t -> true);

        assertThat(predicate.test(new IOException("timeout"))).isFalse();
    }

    @Test
    void allowsRetryForPostWithIdempotencyKey() {
        IntegrationCallContext.set(IntegrationCallContext.forMethod(HttpMethod.POST, "idem-123"));
        IdempotencyAwareRetryPredicate predicate = new IdempotencyAwareRetryPredicate(t -> true);

        assertThat(predicate.test(new IOException("timeout"))).isTrue();
    }
}
