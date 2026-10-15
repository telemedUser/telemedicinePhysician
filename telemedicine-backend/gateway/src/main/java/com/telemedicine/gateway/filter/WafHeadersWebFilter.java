package com.telemedicine.gateway.filter;

import com.telemedicine.common.security.TelemedicineSecurityProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Applies WAF-friendly response headers at the edge. */
@Component
public class WafHeadersWebFilter implements WebFilter, Ordered {

    private final TelemedicineSecurityProperties properties;

    public WafHeadersWebFilter(TelemedicineSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        TelemedicineSecurityProperties.Headers config = properties.getHeaders();
        if (config.isEnabled()) {
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("Referrer-Policy", config.getReferrerPolicy());
            headers.add("Permissions-Policy", config.getPermissionsPolicy());
            headers.add("Content-Security-Policy", config.resolvedContentSecurityPolicy());
            if (config.isHstsEnabled()) {
                headers.add(
                        "Strict-Transport-Security",
                        "max-age="
                                + config.getHstsMaxAge().toSeconds()
                                + "; includeSubDomains");
            }
        }
        headers.remove(HttpHeaders.SERVER);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
