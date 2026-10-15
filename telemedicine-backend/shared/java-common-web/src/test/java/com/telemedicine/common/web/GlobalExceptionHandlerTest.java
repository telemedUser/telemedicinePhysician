package com.telemedicine.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.error.ConflictException;
import com.telemedicine.common.core.error.ConsentRequiredException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.common.core.error.RateLimitedException;
import com.telemedicine.common.core.error.ValidationException;
import jakarta.validation.ConstraintViolationException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.boot.test.context.SpringBootTest(
        classes = {WebTestApplication.class, GlobalExceptionHandlerTest.TestController.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("a domain exception becomes a problem+json document with the catalogue code")
    void domainExceptionBecomesProblemDocument() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "notFound"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.code()))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.type").value("https://telemedicine.bupa/problems/not-found"))
                .andExpect(jsonPath("$.instance").value("/test/throw"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.category").value("NOT_FOUND"))
                .andExpect(jsonPath("$.retryable").value(false));
    }

    @Test
    @DisplayName("the correlation id is echoed on failures, which is what L2 support traces by")
    void echoesCorrelationId() throws Exception {
        mockMvc.perform(
                        get("/test/throw")
                                .param("kind", "conflict")
                                .header(Headers.CORRELATION_ID, "01JCORR"))
                .andExpect(status().isConflict())
                .andExpect(header().string(Headers.CORRELATION_ID, "01JCORR"))
                .andExpect(jsonPath("$.correlationId").value("01JCORR"));
    }

    @Test
    void domainDetailsBecomeExtensionMembers() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "consent"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONSENT_REQUIRED.code()))
                .andExpect(jsonPath("$.consentType").value("PLATFORM_TERMS"));
    }

    @Test
    void validationExceptionCarriesViolations() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("patient.rut"))
                .andExpect(jsonPath("$.violations[0].message").value("must be present"));
    }

    @Test
    void rateLimitSetsRetryAfter() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "rateLimited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "45"))
                .andExpect(jsonPath("$.retryable").value(true));
    }

    @Test
    @DisplayName("bean validation failures list every offending field")
    void beanValidationFailuresBecomeViolations() throws Exception {
        mockMvc.perform(
                        post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\",\"age\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.code()))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(2));
    }

    @Test
    @DisplayName("a rejected value containing PHI is redacted before it is echoed back")
    void redactsRejectedValues() throws Exception {
        mockMvc.perform(
                        post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"12.345.678-5\",\"age\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("name"))
                .andExpect(jsonPath("$.violations[0].rejectedValue").value("[RUT[REDACTED]]"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "12.345.678"))));
    }

    @Test
    void constraintViolationsBecomeViolations() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.code()));
    }

    @Test
    @DisplayName("an unparseable body never quotes the offending JSON back to the caller")
    void unreadableBodyIsGeneric() throws Exception {
        mockMvc.perform(
                        post("/test/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.code()))
                .andExpect(jsonPath("$.detail").value("Request body could not be parsed"));
    }

    @Test
    void typeMismatchNamesTheParameter() throws Exception {
        mockMvc.perform(get("/test/typed").param("count", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("count"))
                .andExpect(jsonPath("$.violations[0].message").value("expected Integer"));
    }

    @Test
    void missingParameterIsReported() throws Exception {
        mockMvc.perform(get("/test/typed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MISSING_PARAMETER.code()))
                .andExpect(jsonPath("$.violations[0].field").value("count"));
    }

    @Test
    void methodNotAllowedListsSupportedMethods() throws Exception {
        mockMvc.perform(post("/test/typed"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.code()))
                .andExpect(jsonPath("$.supportedMethods").isArray());
    }

    @Test
    void unsupportedMediaTypeIsReported() throws Exception {
        mockMvc.perform(post("/test/validate").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.code()));
    }

    @Test
    void oversizedUploadIsReported() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "tooLarge"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAYLOAD_TOO_LARGE.code()));
    }

    @Test
    void unmappedPathReturnsAProblemDocument() throws Exception {
        mockMvc.perform(get("/test/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No handler for this path"));
    }

    @Test
    void responseStatusExceptionKeepsItsStatus() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "responseStatus"))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.status").value(418))
                .andExpect(jsonPath("$.detail").value("brewing"))
                // No catalogue entry, so no category or retryable claim is made.
                .andExpect(jsonPath("$.category").doesNotExist());
    }

    @Test
    @DisplayName("an unexpected fault returns 500 with no stack trace and no internals")
    void unexpectedFaultLeaksNothing() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.code()))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.exceptionMessage").doesNotExist())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "IllegalStateException"))));
    }

    @Test
    void optimisticLockBecomesConflict() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "optimisticLock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.OPTIMISTIC_LOCK_CONFLICT.code()))
                .andExpect(jsonPath("$.retryable").value(true));
    }

    @Test
    @DisplayName("a constraint breach is a conflict, and the driver message is never surfaced")
    void dataIntegrityBecomesConflict() throws Exception {
        mockMvc.perform(get("/test/throw").param("kind", "dataIntegrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONFLICT.code()))
                .andExpect(jsonPath("$.detail").value("The request conflicts with existing data"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "uq_patient_rut"))));
    }

    /** Fixture controller: one endpoint per exception branch under test. */
    @org.springframework.web.bind.annotation.RestController
    @org.springframework.web.bind.annotation.RequestMapping("/test")
    @org.springframework.validation.annotation.Validated
    static class TestController {

        @org.springframework.web.bind.annotation.GetMapping("/throw")
        String throwing(@org.springframework.web.bind.annotation.RequestParam String kind) {
            switch (kind) {
                case "notFound" -> throw new NotFoundException("appointment is gone");
                case "conflict" -> throw new ConflictException("already checked in");
                case "consent" -> throw new ConsentRequiredException("PLATFORM_TERMS");
                case "validation" ->
                        throw ValidationException.field("patient.rut", "must be present");
                case "rateLimited" -> throw new RateLimitedException(Duration.ofSeconds(45));
                case "constraint" ->
                        throw new ConstraintViolationException("invalid", java.util.Set.of());
                case "tooLarge" -> throw new MaxUploadSizeExceededException(1024L);
                case "responseStatus" ->
                        throw new ResponseStatusException(
                                org.springframework.http.HttpStatus.I_AM_A_TEAPOT, "brewing");
                case "optimisticLock" ->
                        throw new OptimisticLockingFailureException("version clash");
                case "dataIntegrity" ->
                        throw new DataIntegrityViolationException(
                                "duplicate key value violates unique constraint"
                                        + " \"uq_patient_rut\"");
                default -> throw new IllegalStateException("connection pool exhausted at db-01");
            }
        }

        @org.springframework.web.bind.annotation.GetMapping("/typed")
        String typed(@org.springframework.web.bind.annotation.RequestParam Integer count) {
            return String.valueOf(count);
        }

        @org.springframework.web.bind.annotation.PostMapping("/validate")
        String validate(
                @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid
                        Payload payload) {
            return payload.name();
        }

        record Payload(
                @jakarta.validation.constraints.NotBlank
                        @jakarta.validation.constraints.Pattern(
                                regexp = "[A-Za-z ]*",
                                message = "must contain letters only")
                        String name,
                @jakarta.validation.constraints.Min(0) int age) {}
    }
}
