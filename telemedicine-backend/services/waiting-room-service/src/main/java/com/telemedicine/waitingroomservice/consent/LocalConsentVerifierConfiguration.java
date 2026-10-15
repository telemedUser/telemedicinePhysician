package com.telemedicine.waitingroomservice.consent;

import com.telemedicine.common.security.consent.ConsentVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalConsentVerifierConfiguration {

    @Bean
    ConsentVerifier consentVerifier(ConsentService consentService) {
        return consentService;
    }
}
