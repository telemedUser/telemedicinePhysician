package com.telemedicine.waitingroomservice.jobs;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import com.telemedicine.waitingroomservice.status.StatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalJobsController.class)
@Import(WaitingRoomServiceProperties.class)
class InternalJobsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private StatusService statusService;

    @Test
    void waitingRoomTimeoutReturnsOk() throws Exception {
        when(statusService.expireTimedOutPatients(org.mockito.ArgumentMatchers.any())).thenReturn(2);

        mockMvc.perform(post("/api/v1/internal/jobs/waiting-room-timeout/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(2))
                .andExpect(jsonPath("$.data.status").value("OK"));
    }

    @Test
    void noShowDetectionReturnsOk() throws Exception {
        when(statusService.detectNoShows(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);

        mockMvc.perform(post("/api/v1/internal/jobs/no-show-detection/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(1));
    }
}
