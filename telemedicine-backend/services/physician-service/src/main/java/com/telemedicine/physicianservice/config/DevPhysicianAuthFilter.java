package com.telemedicine.physicianservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Local-development authentication (dev profile only). The standalone physician app signs in with a
 * dev login and then sends the signed-in physician's id on every request via the {@code
 * X-Physician-Id} header. This filter turns that header into an authenticated principal whose name
 * is the physician id, so {@code SecurityContextAccessor.currentPhysicianId()} resolves to the
 * logged-in doctor and the worklist is scoped to them.
 *
 * <p>When the header is absent it falls back to {@code dev-physician} so seeded demo data and
 * unauthenticated probes still work. This is strictly a dev convenience; in a real environment the
 * {@code dev} profile is absent and the Keycloak JWT chain supplies the identity instead.
 */
public class DevPhysicianAuthFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Physician-Id";
    static final String DEFAULT_PHYSICIAN = "dev-physician";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String physicianId = request.getHeader(HEADER);
        if (physicianId == null || physicianId.isBlank()) {
            physicianId = DEFAULT_PHYSICIAN;
        }
        var authorities =
                List.of(
                        new SimpleGrantedAuthority("ROLE_PHYSICIAN"),
                        new SimpleGrantedAuthority("ROLE_ADMIN"));
        var authentication =
                new UsernamePasswordAuthenticationToken(physicianId.trim(), "N/A", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }
}
