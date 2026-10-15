package com.telemedicine.common.observability.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.MdcKeys;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcPropagatingTaskDecoratorTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void propagatesMdcToAsyncThread() throws Exception {
        MDC.put(MdcKeys.CORRELATION_ID, "async-corr");
        MdcPropagatingTaskDecorator decorator = new MdcPropagatingTaskDecorator();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> asyncValue = new AtomicReference<>();

        try {
            executor.execute(
                    decorator.decorate(
                            () -> {
                                asyncValue.set(MDC.get(MdcKeys.CORRELATION_ID));
                                latch.countDown();
                            }));
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(asyncValue.get()).isEqualTo("async-corr");
        } finally {
            executor.shutdownNow();
        }
    }
}
