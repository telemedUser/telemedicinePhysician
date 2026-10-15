package com.telemedicine.common.security.method;

import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.annotation.RequiresRole;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Grants a {@link RequiresRole}-annotated invocation when the caller holds any listed role.
 *
 * <p>Method-level annotations win over class-level ones, matching how {@code @PreAuthorize}
 * behaves, so a controller can declare a broad default and tighten one endpoint.
 */
public class RequiresRoleAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authentication, MethodInvocation invocation) {
        RequiresRole annotation = annotationOn(invocation);
        if (annotation == null) {
            // No annotation means this manager has nothing to say; abstain rather than deny, so
            // an unannotated method is not blocked by an interceptor that happened to match.
            return null;
        }
        Authentication caller = authentication.get();
        if (caller == null || !caller.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        Set<String> required = requiredAuthorities(annotation);
        for (GrantedAuthority granted : caller.getAuthorities()) {
            if (required.contains(granted.getAuthority())) {
                return new AuthorizationDecision(true);
            }
        }
        return new AuthorizationDecision(false);
    }

    private static Set<String> requiredAuthorities(RequiresRole annotation) {
        Set<String> authorities = new LinkedHashSet<>();
        Arrays.stream(annotation.value())
                .map(Roles::authority)
                .filter(java.util.Objects::nonNull)
                .forEach(authorities::add);
        return authorities;
    }

    private static RequiresRole annotationOn(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        RequiresRole onMethod =
                AnnotatedElementUtils.findMergedAnnotation(method, RequiresRole.class);
        if (onMethod != null) {
            return onMethod;
        }
        Object target = invocation.getThis();
        Class<?> targetClass = target != null ? target.getClass() : method.getDeclaringClass();
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, RequiresRole.class);
    }
}
