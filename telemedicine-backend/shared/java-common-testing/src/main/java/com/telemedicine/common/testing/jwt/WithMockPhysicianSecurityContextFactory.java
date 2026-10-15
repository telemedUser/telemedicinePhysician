package com.telemedicine.common.testing.jwt;

import org.springframework.security.test.context.support.WithSecurityContextFactory;

final class WithMockPhysicianSecurityContextFactory
        implements WithSecurityContextFactory<WithMockPhysician> {

    @Override
    public org.springframework.security.core.context.SecurityContext createSecurityContext(
            WithMockPhysician annotation) {
        return WithMockJwtSecurityContextFactory.contextFor(TestJwts.physician());
    }
}
