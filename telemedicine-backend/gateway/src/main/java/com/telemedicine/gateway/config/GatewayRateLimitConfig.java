package com.telemedicine.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRateLimitConfig {

    @Bean
    @Primary
    RedisRateLimiter defaultRedisRateLimiter(
            @Value("${telemedicine.gateway.rate-limit.replenish-rate:100}") int replenishRate,
            @Value("${telemedicine.gateway.rate-limit.burst-capacity:200}") int burstCapacity) {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }

    @Bean
    RedisRateLimiter strictRedisRateLimiter() {
        return new RedisRateLimiter(30, 60);
    }

    @Bean
    @Primary
    KeyResolver userKeyResolver() {
        return exchange -> {
            String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (auth != null && !auth.isBlank()) {
                return Mono.just(auth);
            }
            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }
            return Mono.just("anonymous");
        };
    }

    @Bean
    KeyResolver pathKeyResolver(KeyResolver userKeyResolver) {
        return exchange ->
                userKeyResolver
                        .resolve(exchange)
                        .map(key -> exchange.getRequest().getPath().value() + ":" + key);
    }
}
