package com.telemedicine.common.integration.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.security.TelemedicineSecurityProperties;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

class IntegrationAutoConfigurationExtendedTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(IntegrationAutoConfiguration.class))
                    .withUserConfiguration(SecuritySupport.class);

    @Test
    void wiresOptionalPropagationBeansWhenDependenciesPresent() {
        runner.run(
                context -> {
                    assertThat(context).hasBean("telemedicineIntegrationRestInterceptors");
                    assertThat(context).hasBean("telemedicineIntegrationWebFilters");
                    assertThat(context).hasBean("telemedicineIntegrationAuthInterceptor");
                    assertThat(context).hasBean("telemedicineIntegrationAuthWebFilter");
                    List<ClientHttpRequestInterceptor> rest =
                            context.getBean("telemedicineIntegrationRestInterceptors", List.class);
                    List<ExchangeFilterFunction> web =
                            context.getBean("telemedicineIntegrationWebFilters", List.class);
                    assertThat(rest).isNotEmpty();
                    assertThat(web).isNotEmpty();
                });
    }

    @Configuration
    static class SecuritySupport {

        @Bean
        SecurityContextAccessor securityContextAccessor() {
            return new SecurityContextAccessor();
        }

        @Bean
        TelemedicineSecurityProperties telemedicineSecurityProperties() {
            return new TelemedicineSecurityProperties();
        }
    }
}
