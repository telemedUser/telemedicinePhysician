package com.telemedicine.configservice.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.core.error.ValidationException;
import org.junit.jupiter.api.Test;

class ConfigKeyValidatorTest {

    @Test
    void acceptsKnownProviderKey() {
        assertThatCode(() -> ConfigKeyValidator.validate("video.provider", "simulator"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidProvider() {
        assertThatThrownBy(() -> ConfigKeyValidator.validate("video.provider", "unknown-vendor"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsUnknownKey() {
        assertThatThrownBy(() -> ConfigKeyValidator.validate("not.a.real.key", "value"))
                .isInstanceOf(ValidationException.class);
    }
}
