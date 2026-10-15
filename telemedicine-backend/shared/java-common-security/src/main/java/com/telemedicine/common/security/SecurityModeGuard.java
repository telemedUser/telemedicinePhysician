package com.telemedicine.common.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

/**
 * Refuses to start when the authentication bypass is active on a protected profile.
 *
 * <p>{@link SecurityMode#DEV} permits every request. Shipping it by accident — a stale environment
 * variable, a copied compose file, a profile that was never switched — exposes every endpoint of
 * the service, and nothing in the running system looks wrong afterwards. A failed startup is loud,
 * immediate and recoverable; a silently open service is none of those.
 */
public class SecurityModeGuard implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SecurityModeGuard.class);

    private final TelemedicineSecurityProperties properties;
    private final Environment environment;

    public SecurityModeGuard(TelemedicineSecurityProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        verify(properties, activeProfiles());
    }

    private Set<String> activeProfiles() {
        return new LinkedHashSet<>(Arrays.asList(environment.getActiveProfiles()));
    }

    /**
     * @throws IllegalStateException when dev mode is active on one of the protected profiles
     */
    static void verify(TelemedicineSecurityProperties properties, Set<String> activeProfiles) {
        if (properties.getMode() != SecurityMode.DEV) {
            return;
        }
        List<String> protectedProfiles =
                properties.getProtectedProfiles() == null
                        ? List.of()
                        : properties.getProtectedProfiles();
        List<String> violations =
                protectedProfiles.stream().filter(activeProfiles::contains).toList();
        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "telemedicine.security.mode=DEV disables authentication, and the active"
                            + " profiles "
                            + violations
                            + " are declared protected. Set telemedicine.security.mode=JWT, or"
                            + " remove the profile from"
                            + " telemedicine.security.protected-profiles if this really is a"
                            + " non-production environment.");
        }
        log.warn(
                "telemedicine.security.mode=DEV: every request is permitted without"
                        + " authentication. Active profiles: {}.",
                activeProfiles.isEmpty() ? "(default)" : activeProfiles);
    }
}
