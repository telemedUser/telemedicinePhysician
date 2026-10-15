package com.telemedicine.common.integration.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class IntegrationCallContextTest {

    @Test
    void exposesMetadataAndHeaderName() {
        IntegrationCallContext.set(IntegrationCallContext.forMethod(HttpMethod.PUT, "key-1"));
        assertThat(IntegrationCallContext.currentCallIsRetrySafe()).isTrue();
        assertThat(IntegrationCallContext.get().method()).isEqualTo(HttpMethod.PUT);
        assertThat(IntegrationCallContext.idempotencyHeaderName()).isEqualTo("X-Idempotency-Key");
        IntegrationCallContext.clear();
        assertThat(IntegrationCallContext.currentCallIsRetrySafe()).isFalse();
    }
}
