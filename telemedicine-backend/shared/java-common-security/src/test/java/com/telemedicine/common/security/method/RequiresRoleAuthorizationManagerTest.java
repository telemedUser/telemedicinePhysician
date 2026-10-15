package com.telemedicine.common.security.method;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.annotation.RequiresRole;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.ReflectionUtils;

class RequiresRoleAuthorizationManagerTest {

    private final RequiresRoleAuthorizationManager manager = new RequiresRoleAuthorizationManager();

    @Test
    void grantsWhenTheCallerHoldsOneOfTheListedRoles() {
        assertThat(decide("clinicalNote", Roles.ADMIN).isGranted()).isTrue();
        assertThat(decide("clinicalNote", Roles.PHYSICIAN).isGranted()).isTrue();
    }

    @Test
    void deniesWhenTheCallerHoldsNoneOfThem() {
        assertThat(decide("clinicalNote", Roles.PATIENT).isGranted()).isFalse();
    }

    @Test
    void deniesAnUnauthenticatedCaller() {
        assertThat(check("clinicalNote", () -> null).isGranted()).isFalse();
        assertThat(
                        check(
                                        "clinicalNote",
                                        () ->
                                                new AnonymousAuthenticationToken(
                                                        "key",
                                                        "anonymousUser",
                                                        List.of(
                                                                new SimpleGrantedAuthority(
                                                                        "ROLE_ANONYMOUS"))))
                                .isGranted())
                .isFalse();
    }

    @Test
    @DisplayName("bare realm names in the annotation are matched against ROLE_ authorities")
    void acceptsBareAndPrefixedRoleNamesAlike() {
        assertThat(decide("prefixed", Roles.ADMIN).isGranted()).isTrue();
    }

    @Test
    @DisplayName("an unannotated method is abstained on, not denied")
    void abstainsWithoutAnAnnotation() {
        assertThat(decide("unguarded", Roles.PATIENT)).isNull();
    }

    @Test
    void fallsBackToAClassLevelAnnotation() {
        AuthorizationDecision decision =
                manager.check(
                        () -> authentication(Roles.ADMIN),
                        invocation(AdminOnlyService.class, new AdminOnlyService(), "anything"));

        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    @DisplayName("the method annotation wins over a broader one on the class")
    void prefersTheMethodAnnotation() {
        AuthorizationDecision decision =
                manager.check(
                        () -> authentication(Roles.PATIENT),
                        invocation(AdminOnlyService.class, new AdminOnlyService(), "alsoPatients"));

        assertThat(decision.isGranted()).isTrue();
    }

    private AuthorizationDecision decide(String methodName, String authority) {
        return check(methodName, () -> authentication(authority));
    }

    private AuthorizationDecision check(
            String methodName, Supplier<Authentication> authentication) {
        return manager.check(
                authentication, invocation(GuardedService.class, new GuardedService(), methodName));
    }

    private static Authentication authentication(String authority) {
        return new TestingAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority(authority)));
    }

    private static MethodInvocation invocation(Class<?> type, Object target, String methodName) {
        Method method = ReflectionUtils.findMethod(type, methodName);
        return new SimpleMethodInvocation(target, method);
    }

    static class GuardedService {

        @RequiresRole({"PHYSICIAN", "ADMIN"})
        void clinicalNote() {}

        @RequiresRole({"ROLE_ADMIN"})
        void prefixed() {}

        void unguarded() {}
    }

    @RequiresRole("ADMIN")
    static class AdminOnlyService {

        void anything() {}

        @RequiresRole("PATIENT")
        void alsoPatients() {}
    }

    /** Minimal invocation: the manager only reads the method and the target type. */
    record SimpleMethodInvocation(Object target, Method method) implements MethodInvocation {

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return new Object[0];
        }

        @Override
        public Object proceed() {
            return null;
        }

        @Override
        public Object getThis() {
            return target;
        }

        @Override
        public java.lang.reflect.AccessibleObject getStaticPart() {
            return method;
        }
    }
}
