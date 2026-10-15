package com.telemedicine.common.audit;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.error.ForbiddenException;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.core.phi.PhiRedactor;
import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
public class AuditedAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditedAspect.class);
    private static final String DISPATCH_FAILURE_METRIC = "telemedicine.audit.dispatch.failures";

    private final AuditEventDispatcher dispatcher;
    private final SecurityContextAccessor securityContext;
    private final AuditChainState chainState;
    private final TelemedicineAuditProperties properties;
    private final Optional<MeterRegistry> meterRegistry;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();

    public AuditedAspect(
            AuditEventDispatcher dispatcher,
            SecurityContextAccessor securityContext,
            AuditChainState chainState,
            TelemedicineAuditProperties properties,
            Optional<MeterRegistry> meterRegistry) {
        this.dispatcher = dispatcher;
        this.securityContext = securityContext;
        this.chainState = chainState;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    // Throwable is imposed by ProceedingJoinPoint.proceed().
    @SuppressWarnings("checkstyle:IllegalThrows")
    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object[] args = joinPoint.getArgs();
        boolean captureArgs = audited.includeArgs();
        boolean phiSafe = audited.phiSafe() && properties.isPhiSafeByDefault();
        String beforeHash = captureArgs ? hashArgs(args, phiSafe) : null;

        AuditOutcome outcome = AuditOutcome.SUCCESS;
        String reason = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (AccessDeniedException | ForbiddenException denied) {
            outcome = AuditOutcome.DENIED;
            reason = PhiRedactor.redact(denied.getMessage());
            throw denied;
        } catch (Throwable throwable) {
            outcome = audited.outcomeOnException();
            reason = PhiRedactor.redact(throwable.getMessage());
            throw throwable;
        } finally {
            String afterHash = captureArgs ? hashArgs(result, phiSafe) : null;
            dispatchEvent(joinPoint, audited, outcome, reason, beforeHash, afterHash, phiSafe);
        }
    }

    private void dispatchEvent(
            ProceedingJoinPoint joinPoint,
            Audited audited,
            AuditOutcome outcome,
            String reason,
            String beforeHash,
            String afterHash,
            boolean phiSafe) {
        try {
            Instant occurredAt = Instant.now();
            AuditActor actor = resolveActor();
            String resourceId = resolveResourceId(audited, joinPoint);
            AuditChainState.ChainLink link = chainState.nextLink();
            String hash =
                    AuditHasher.hash(
                            actor.id(),
                            audited.action(),
                            audited.resourceType(),
                            resourceId,
                            outcome.name(),
                            occurredAt,
                            link.previousHash());

            AuditEvent event =
                    new AuditEvent(
                            IdGenerator.newId(),
                            actor,
                            audited.action(),
                            audited.resourceType(),
                            phiSafe ? PhiRedactor.redact(resourceId) : resourceId,
                            outcome,
                            reason,
                            beforeHash,
                            afterHash,
                            correlationId(),
                            occurredAt,
                            link.sequence(),
                            link.previousHash(),
                            hash);
            dispatcher.dispatch(event);
            chainState.advance(hash);
        } catch (RuntimeException exception) {
            meterRegistry.ifPresent(
                    registry -> registry.counter(DISPATCH_FAILURE_METRIC).increment());
            log.error(
                    "Failed to dispatch audit event for action={} resourceType={}",
                    audited.action(),
                    audited.resourceType(),
                    exception);
        }
    }

    private AuditActor resolveActor() {
        if (!securityContext.isAuthenticated()) {
            return new AuditActor(
                    properties.getSystemActorId(),
                    properties.getSystemActorRole(),
                    clientIp().orElse(null),
                    userAgent().orElse(null));
        }
        return new AuditActor(
                securityContext.currentUserId().orElse(properties.getSystemActorId()),
                primaryRole(securityContext.currentRoles()),
                clientIp().orElse(null),
                userAgent().orElse(null));
    }

    private String resolveResourceId(Audited audited, ProceedingJoinPoint joinPoint) {
        if (audited.resourceIdExpression().isBlank()) {
            return joinPoint.getSignature().getName();
        }
        Object value =
                parser.parseExpression(audited.resourceIdExpression())
                        .getValue(evaluationContext(joinPoint));
        return value == null ? joinPoint.getSignature().getName() : value.toString();
    }

    private StandardEvaluationContext evaluationContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getThis());
        Method method = signature.getMethod();
        String[] names = parameterNames.getParameterNames(method);
        Object[] args = joinPoint.getArgs();
        if (names != null) {
            for (int index = 0; index < names.length && index < args.length; index++) {
                context.setVariable(names[index], args[index]);
            }
        }
        context.setVariable("args", args);
        return context;
    }

    private static String hashArgs(Object value, boolean phiSafe) {
        if (phiSafe && value instanceof Object[] args) {
            Object[] redacted = new Object[args.length];
            for (int index = 0; index < args.length; index++) {
                redacted[index] = redactValue(args[index]);
            }
            return AuditHasher.contentHash(redacted);
        }
        if (phiSafe) {
            return AuditHasher.contentHash(redactValue(value));
        }
        return AuditHasher.contentHash(value);
    }

    private static Object redactValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return PhiRedactor.redact(text);
        }
        return value;
    }

    private static String primaryRole(Set<String> roles) {
        if (roles.contains(Roles.ADMIN)) {
            return Roles.REALM_ADMIN;
        }
        if (roles.contains(Roles.PHYSICIAN)) {
            return Roles.REALM_PHYSICIAN;
        }
        if (roles.contains(Roles.PATIENT)) {
            return Roles.REALM_PATIENT;
        }
        if (roles.contains(Roles.SERVICE)) {
            return Roles.REALM_SERVICE;
        }
        return "SYSTEM";
    }

    private static String correlationId() {
        String fromMdc = MDC.get(MdcKeys.CORRELATION_ID);
        return fromMdc != null ? fromMdc : IdGenerator.correlationId();
    }

    private static Optional<HttpServletRequest> currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return Optional.of(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    private static Optional<String> clientIp() {
        return currentRequest()
                .map(
                        request -> {
                            String forwarded = request.getHeader("X-Forwarded-For");
                            if (forwarded != null && !forwarded.isBlank()) {
                                return forwarded.split(",")[0].trim();
                            }
                            return request.getRemoteAddr();
                        });
    }

    private static Optional<String> userAgent() {
        return currentRequest().map(request -> request.getHeader("User-Agent"));
    }
}
