package com.telemedicine.common.persistence.audit;

import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;

/** Resolves the JPA auditing principal from the security context. */
public class TelemedicineAuditorAware implements AuditorAware<String> {

    private final SecurityContextAccessor securityContextAccessor;

    public TelemedicineAuditorAware(SecurityContextAccessor securityContextAccessor) {
        this.securityContextAccessor = securityContextAccessor;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        return securityContextAccessor.currentUserId();
    }
}
