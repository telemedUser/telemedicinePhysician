package com.telemedicine.common.web.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.telemedicine.common.core.constants.Headers;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.web.WebTestApplication;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        classes = {WebTestApplication.class, IdempotencyKeyFilterTest.PaymentController.class},
        properties = {
            "telemedicine.web.idempotency.enabled=true",
            "telemedicine.web.idempotency.path-patterns=/v1/**",
            "telemedicine.web.request-logging.enabled=false"
        })
@AutoConfigureMockMvc
class IdempotencyKeyFilterTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentController controller;
    @Autowired private IdempotencyStore store;

    @BeforeEach
    void reset() {
        controller.executions.set(0);
    }

    @Test
    @DisplayName("a retried request executes once and replays the first response")
    void replaysTheStoredResponse() throws Exception {
        String key = "key-" + System.nanoTime();

        mockMvc.perform(charge(key, "{\"amount\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.execution").value(1))
                .andExpect(header().doesNotExist("X-Idempotent-Replay"));

        mockMvc.perform(charge(key, "{\"amount\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.execution").value(1))
                .andExpect(header().string("X-Idempotent-Replay", "true"));

        assertThat(controller.executions.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("reusing a key for a different body is a conflict, not a wrong replay")
    void rejectsAKeyReusedForADifferentRequest() throws Exception {
        String key = "key-" + System.nanoTime();

        mockMvc.perform(charge(key, "{\"amount\":1000}")).andExpect(status().isOk());

        mockMvc.perform(charge(key, "{\"amount\":9999}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCode.IDEMPOTENCY_KEY_CONFLICT.code()))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "This idempotency key was already used for a different"
                                                + " request"));

        assertThat(controller.executions.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("a failed request releases its key so the client can retry")
    void doesNotPinFailuresToTheKey() throws Exception {
        String key = "key-" + System.nanoTime();

        mockMvc.perform(charge(key, "{\"amount\":-1}")).andExpect(status().isBadRequest());

        assertThat(store.find(key)).isEmpty();

        mockMvc.perform(charge(key, "{\"amount\":-1}")).andExpect(status().isBadRequest());
        assertThat(controller.executions.get()).isEqualTo(2);
    }

    @Test
    void passesThroughWhenNoKeyIsSuppliedAndNoneIsRequired() throws Exception {
        mockMvc.perform(
                        post("/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\":1000}"))
                .andExpect(status().isOk());

        assertThat(controller.executions.get()).isEqualTo(1);
    }

    @Test
    void ignoresSafeMethodsAndUnmatchedPaths() throws Exception {
        mockMvc.perform(get("/v1/payments")).andExpect(status().isMethodNotAllowed());

        mockMvc.perform(
                        post("/internal/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\":1000}"))
                .andExpect(status().isOk());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            charge(String key, String body) {
        return post("/v1/payments")
                .header(Headers.IDEMPOTENCY_KEY, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @RestController
    @RequestMapping
    static class PaymentController {

        final AtomicInteger executions = new AtomicInteger();

        @PostMapping({"/v1/payments", "/internal/payments"})
        Object charge(@RequestBody Charge charge) {
            int execution = executions.incrementAndGet();
            if (charge.amount() < 0) {
                throw new com.telemedicine.common.core.error.ValidationException(
                        "amount must be positive");
            }
            return java.util.Map.of("execution", execution, "amount", charge.amount());
        }

        record Charge(int amount) {}
    }
}
