package com.telemedicine.common.persistence.cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

class DegradingCacheErrorHandlerTest {

    private final DegradingCacheErrorHandler handler = new DegradingCacheErrorHandler();

    @Test
    void swallowsCacheErrors() {
        Cache cache = mock(Cache.class);
        RuntimeException failure = new RuntimeException("redis down");

        assertThatCode(() -> handler.handleCacheGetError(failure, cache, "key"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCachePutError(failure, cache, "key", "value"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheEvictError(failure, cache, "key"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(failure, cache))
                .doesNotThrowAnyException();
    }
}
