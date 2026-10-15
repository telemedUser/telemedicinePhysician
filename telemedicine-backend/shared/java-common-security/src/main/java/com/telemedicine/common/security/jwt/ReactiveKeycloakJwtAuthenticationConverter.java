package com.telemedicine.common.security.jwt;

import java.util.Collection;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

/**
 * Reactive JWT converter delegating role mapping to {@link KeycloakRoleConverter}.
 *
 * <p>Used by the WebFlux gateway so role mapping stays identical to servlet services.
 */
public class ReactiveKeycloakJwtAuthenticationConverter
        implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private final KeycloakRoleConverter delegate;

    public ReactiveKeycloakJwtAuthenticationConverter(KeycloakRoleConverter delegate) {
        this.delegate = delegate;
    }

    public ReactiveKeycloakJwtAuthenticationConverter() {
        this(new KeycloakRoleConverter());
    }

    public ReactiveKeycloakJwtAuthenticationConverter(java.util.List<String> roleClients) {
        this(new KeycloakRoleConverter(roleClients));
    }

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = delegate.convert(jwt);
        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }
}
