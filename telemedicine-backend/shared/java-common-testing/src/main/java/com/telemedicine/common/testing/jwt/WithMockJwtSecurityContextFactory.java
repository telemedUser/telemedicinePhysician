package com.telemedicine.common.testing.jwt;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

final class WithMockJwtSecurityContextFactory
        implements WithSecurityContextFactory<WithMockPatient> {

    @Override
    public SecurityContext createSecurityContext(WithMockPatient annotation) {
        return contextFor(TestJwts.patient());
    }

    static SecurityContext contextFor(Jwt jwt) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt, TestJwts.authorities(jwt)));
        return context;
    }
}
