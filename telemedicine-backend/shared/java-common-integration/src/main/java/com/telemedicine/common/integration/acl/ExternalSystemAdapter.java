package com.telemedicine.common.integration.acl;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.IntegrationException;
import com.telemedicine.common.core.error.ProviderOutageException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for outbound adapters calling external systems.
 *
 * <p>Subclasses implement {@link #invoke(Object)} with the wire call; this class handles ACL
 * mapping, exception translation and {@link IntegrationFailed} emission.
 *
 * @param <REQ> domain request
 * @param <EXT_REQ> wire request
 * @param <EXT_RES> wire response
 * @param <RES> domain response
 */
public abstract class ExternalSystemAdapter<REQ, EXT_REQ, EXT_RES, RES> {

    private static final Logger log = LoggerFactory.getLogger(ExternalSystemAdapter.class);

    private final String system;
    private final IntegrationFailureReporter failureReporter;
    private final AntiCorruptionMapper<REQ, EXT_REQ, EXT_RES, RES> mapper;

    protected ExternalSystemAdapter(
            String system,
            IntegrationFailureReporter failureReporter,
            AntiCorruptionMapper<REQ, EXT_REQ, EXT_RES, RES> mapper) {
        this.system = Objects.requireNonNull(system, "system");
        this.failureReporter = Objects.requireNonNull(failureReporter, "failureReporter");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    /** Logical external system key, for example {@code fonasa}. */
    public String system() {
        return system;
    }

    /**
     * Executes the adapter call with ACL translation and failure reporting.
     *
     * @param request domain request
     * @param operation logical operation name for events and logs
     * @param subjectRef pseudonymous subject reference, usually an appointment id
     */
    public RES execute(REQ request, String operation, String subjectRef) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(operation, "operation");
        try {
            EXT_REQ externalRequest = mapper.toExternal(request);
            EXT_RES externalResponse = invoke(externalRequest);
            return mapper.toDomain(externalResponse);
        } catch (RuntimeException exception) {
            ErrorCode code = mapErrorCode(exception);
            failureReporter.report(system, operation, subjectRef, code);
            throw translate(exception, code);
        }
    }

    /** Performs the outbound wire call with an already-translated request. */
    protected abstract EXT_RES invoke(EXT_REQ externalRequest);

    protected RuntimeException translate(RuntimeException exception, ErrorCode code) {
        if (exception instanceof IntegrationException
                || exception instanceof ProviderOutageException) {
            return exception;
        }
        if (code == ErrorCode.PROVIDER_OUTAGE) {
            return new ProviderOutageException(
                    ErrorCode.PROVIDER_OUTAGE, system, exception.getMessage(), exception);
        }
        return new IntegrationException(code, system, exception.getMessage(), exception);
    }

    protected ErrorCode mapErrorCode(RuntimeException exception) {
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
        log.debug("Unmapped integration exception for system={}", system, exception);
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
