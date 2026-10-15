package com.telemedicine.common.security.consent;

import com.telemedicine.common.core.error.ConsentRequiredException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/** Applies {@link RequiresConsent} by consulting the configured {@link ConsentVerifier}. */
@Aspect
public class ConsentGuardAspect {

    private final ConsentVerifier verifier;
    private final SecurityContextAccessor securityContext;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();

    public ConsentGuardAspect(ConsentVerifier verifier, SecurityContextAccessor securityContext) {
        this.verifier = verifier;
        this.securityContext = securityContext;
    }

    // Throwable is imposed by ProceedingJoinPoint.proceed(); narrowing it would mean wrapping or
    // swallowing whatever the advised method threw.
    @SuppressWarnings("checkstyle:IllegalThrows")
    @Around(
            "@annotation(com.telemedicine.common.security.consent.RequiresConsent)"
                    + " || @within(com.telemedicine.common.security.consent.RequiresConsent)")
    public Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresConsent annotation = annotationFor(joinPoint);
        if (annotation == null) {
            return joinPoint.proceed();
        }
        String subjectId = resolveSubject(annotation, joinPoint);
        if (subjectId == null || subjectId.isBlank()) {
            // Refusing here is the safe answer: an unresolvable subject means the check cannot be
            // performed, and silently proceeding would let an unconsented caller through.
            throw new ConsentRequiredException(
                    ErrorCode.CONSENT_REQUIRED,
                    annotation.value(),
                    "Consent could not be verified because no subject was resolved");
        }
        String requiredVersion = annotation.version().isBlank() ? null : annotation.version();
        ConsentDecision decision = verifier.verify(subjectId, annotation.value(), requiredVersion);
        if (decision == null || !decision.granted()) {
            throw refusal(annotation, decision);
        }
        return joinPoint.proceed();
    }

    private static ConsentRequiredException refusal(
            RequiresConsent annotation, ConsentDecision decision) {
        if (decision == null) {
            return new ConsentRequiredException(annotation.value());
        }
        ErrorCode reason =
                decision.reason() == null ? ErrorCode.CONSENT_REQUIRED : decision.reason();
        if (reason == ErrorCode.CONSENT_WITHDRAWN) {
            return ConsentRequiredException.withdrawn(annotation.value());
        }
        if (reason == ErrorCode.CONSENT_VERSION_OUTDATED) {
            String version =
                    decision.requiredVersion() == null
                            ? annotation.version()
                            : decision.requiredVersion();
            return ConsentRequiredException.outdated(annotation.value(), version);
        }
        return new ConsentRequiredException(annotation.value());
    }

    private String resolveSubject(RequiresConsent annotation, ProceedingJoinPoint joinPoint) {
        if (annotation.subject().isBlank()) {
            return securityContext.currentPatientId().orElse(null);
        }
        Object value = parser.parseExpression(annotation.subject()).getValue(context(joinPoint));
        return value == null ? null : value.toString();
    }

    private EvaluationContext context(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StandardEvaluationContext context = new StandardEvaluationContext(joinPoint.getThis());
        String[] names = parameterNames.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();
        if (names != null) {
            for (int i = 0; i < names.length && i < args.length; i++) {
                context.setVariable(names[i], args[i]);
            }
        }
        return context;
    }

    private static RequiresConsent annotationFor(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequiresConsent onMethod =
                AnnotatedElementUtils.findMergedAnnotation(method, RequiresConsent.class);
        if (onMethod != null) {
            return onMethod;
        }
        Object target = joinPoint.getThis();
        Class<?> targetClass = target != null ? target.getClass() : method.getDeclaringClass();
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, RequiresConsent.class);
    }
}
