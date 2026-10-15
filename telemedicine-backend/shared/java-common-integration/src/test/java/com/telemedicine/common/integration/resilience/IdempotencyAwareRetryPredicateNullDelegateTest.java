package com.telemedicine.common.integration.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class IdempotencyAwareRetryPredicateNullDelegateTest {

    @Test
    void usesTrueDelegateWhenNull() {
        IdempotencyAwareRetryPredicate predicate = new IdempotencyAwareRetryPredicate(null);
        IntegrationCallContext.set(
                IntegrationCallContext.forMethod(org.springframework.http.HttpMethod.GET, null));
        assertThat(predicate.test(new IOException())).isTrue();
    }
}
