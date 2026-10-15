package com.telemedicine.integrationservice.adapter;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.IntegrationException;
import com.telemedicine.common.core.error.ProviderOutageException;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.function.Supplier;

final class AdapterSupport {

    private AdapterSupport() {}

    static <T> T execute(
            IntegrationFailureReporter failureReporter,
            String system,
            String operation,
            String subjectRef,
            Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException exception) {
            ErrorCode code = mapErrorCode(exception);
            failureReporter.report(system, operation, subjectRef, code);
            throw translate(system, exception, code);
        }
    }

    static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "lb://external-simulators";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static RuntimeException translate(
            String system, RuntimeException exception, ErrorCode code) {
        if (exception instanceof IntegrationException || exception instanceof ProviderOutageException) {
            return exception;
        }
        if (code == ErrorCode.PROVIDER_OUTAGE) {
            return new ProviderOutageException(
                    ErrorCode.PROVIDER_OUTAGE, system, exception.getMessage(), exception);
        }
        return new IntegrationException(code, system, exception.getMessage(), exception);
    }

    private static ErrorCode mapErrorCode(RuntimeException exception) {
        if (exception instanceof CallNotPermittedException) {
            return ErrorCode.CIRCUIT_OPEN;
        }
        if (exception instanceof ProviderOutageException providerOutage) {
            return providerOutage.getCode();
        }
        if (exception instanceof IntegrationException integration) {
            return integration.getCode();
        }
        if (hasCause(exception, java.util.concurrent.TimeoutException.class)) {
            return ErrorCode.EXTERNAL_TIMEOUT;
        }
        return ErrorCode.INTEGRATION_ERROR;
    }

    private static boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
