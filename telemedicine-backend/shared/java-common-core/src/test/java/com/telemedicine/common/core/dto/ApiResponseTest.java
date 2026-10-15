package com.telemedicine.common.core.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.error.ErrorCode;
import java.lang.reflect.RecordComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void okWrapsThePayload() {
        ApiResponse<String> response = ApiResponse.ok("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.message()).isNull();
    }

    @Test
    void okSupportsVoidAndAdvisoryMessages() {
        assertThat(ApiResponse.ok().success()).isTrue();
        assertThat(ApiResponse.ok().data()).isNull();

        ApiResponse<String> advisory = ApiResponse.ok("partial", "some providers timed out");

        assertThat(advisory.success()).isTrue();
        assertThat(advisory.message()).isEqualTo("some providers timed out");
    }

    @Test
    void errorCarriesTheCatalogueCodeOnTheWire() {
        assertThat(ApiResponse.error("plain").message()).isEqualTo("plain");
        assertThat(ApiResponse.error(ErrorCode.CONSENT_REQUIRED, "accept the terms").message())
                .isEqualTo("TM-CNS-001: accept the terms");
        assertThat(ApiResponse.error("plain").success()).isFalse();
    }

    @Test
    @DisplayName("the wire shape is frozen: three components, mirrored in three other languages")
    void shapeIsFrozen() {
        RecordComponent[] components = ApiResponse.class.getRecordComponents();

        assertThat(components).hasSize(3);
        assertThat(components[0].getName()).isEqualTo("success");
        assertThat(components[1].getName()).isEqualTo("data");
        assertThat(components[2].getName()).isEqualTo("message");
    }

    @Test
    void serialisesToTheDocumentedJson() throws Exception {
        String json = objectMapper.writeValueAsString(ApiResponse.ok(42));

        assertThat(json).isEqualTo("{\"success\":true,\"data\":42,\"message\":null}");
    }
}
