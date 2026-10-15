package com.telemedicine.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TelemedicineWebPropertiesTest {

    private final TelemedicineWebProperties properties = new TelemedicineWebProperties();

    @Test
    @DisplayName("the defaults are the safe ones: on, no internals leaked, idempotency opt-in")
    void defaultsAreSafe() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isExposeInternalErrors()).isFalse();
        assertThat(properties.getPagination().getDefaultSize()).isEqualTo(20);
        assertThat(properties.getPagination().getMaxSize()).isEqualTo(100);
        assertThat(properties.getIdempotency().isEnabled()).isFalse();
        assertThat(properties.getIdempotency().isRequired()).isFalse();
        assertThat(properties.getIdempotency().getTtl()).isEqualTo(Duration.ofHours(24));
        assertThat(properties.getIdempotency().getMethods()).containsExactly("POST", "PATCH");
        assertThat(properties.getIdempotency().getPathPatterns()).containsExactly("/v1/**");
        assertThat(properties.getRequestLogging().isEnabled()).isTrue();
        assertThat(properties.getRequestLogging().getSampleRate()).isEqualTo(1.0);
        assertThat(properties.getRequestLogging().getExcludePatterns())
                .containsExactly("/actuator/**", "/favicon.ico");
        assertThat(properties.getRequestLogging().getSlowRequestThreshold())
                .isEqualTo(Duration.ofMillis(300));
        assertThat(properties.getEtag().isEnabled()).isFalse();
    }

    @Test
    void everySettingIsWritable() {
        properties.setEnabled(false);
        properties.setExposeInternalErrors(true);
        properties.getPagination().setDefaultSize(5);
        properties.getPagination().setMaxSize(50);
        properties.getIdempotency().setEnabled(true);
        properties.getIdempotency().setRequired(true);
        properties.getIdempotency().setTtl(Duration.ofMinutes(30));
        properties.getIdempotency().setMethods(List.of("PUT"));
        properties.getIdempotency().setPathPatterns(List.of("/v2/**"));
        properties.getRequestLogging().setEnabled(false);
        properties.getRequestLogging().setSampleRate(0.25);
        properties.getRequestLogging().setExcludePatterns(List.of("/health"));
        properties.getRequestLogging().setSlowRequestThreshold(Duration.ofSeconds(1));
        properties.getEtag().setEnabled(true);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isExposeInternalErrors()).isTrue();
        assertThat(properties.getPagination().getDefaultSize()).isEqualTo(5);
        assertThat(properties.getPagination().getMaxSize()).isEqualTo(50);
        assertThat(properties.getIdempotency().isEnabled()).isTrue();
        assertThat(properties.getIdempotency().isRequired()).isTrue();
        assertThat(properties.getIdempotency().getTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(properties.getIdempotency().getMethods()).containsExactly("PUT");
        assertThat(properties.getIdempotency().getPathPatterns()).containsExactly("/v2/**");
        assertThat(properties.getRequestLogging().isEnabled()).isFalse();
        assertThat(properties.getRequestLogging().getSampleRate()).isEqualTo(0.25);
        assertThat(properties.getRequestLogging().getExcludePatterns()).containsExactly("/health");
        assertThat(properties.getRequestLogging().getSlowRequestThreshold())
                .isEqualTo(Duration.ofSeconds(1));
        assertThat(properties.getEtag().isEnabled()).isTrue();
    }
}
