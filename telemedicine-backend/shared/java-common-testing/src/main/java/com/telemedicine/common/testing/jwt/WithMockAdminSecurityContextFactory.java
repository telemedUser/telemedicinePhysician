package com.telemedicine.common.testing.jwt;

import org.springframework.security.test.context.support.WithSecurityContextFactory;

final class WithMockAdminSecurityContextFactory
        implements WithSecurityContextFactory<WithMockAdmin> {

    @Override
    public org.springframework.security.core.context.SecurityContext createSecurityContext(
            WithMockAdmin annotation) {
        return WithMockJwtSecurityContextFactory.contextFor(TestJwts.admin());
    }
}
