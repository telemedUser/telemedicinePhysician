package com.telemedicine.common.observability.mdc;

import org.springframework.core.task.TaskDecorator;

/** Copies the caller MDC into async worker threads. */
public class MdcPropagatingTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        MdcContext context = MdcContext.capture();
        return () -> {
            AutoCloseable scope = context.scope();
            try {
                runnable.run();
            } finally {
                try {
                    scope.close();
                } catch (Exception ignored) {
                    // restoring MDC must not mask the original task failure
                }
            }
        };
    }
}
