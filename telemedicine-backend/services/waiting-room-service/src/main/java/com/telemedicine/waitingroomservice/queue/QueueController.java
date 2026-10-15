package com.telemedicine.waitingroomservice.queue;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPatient;
import com.telemedicine.waitingroomservice.web.dto.QueuePositionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/waiting-room/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/{appointmentId}")
    @IsPatient
    public ApiResponse<QueuePositionResponse> position(@PathVariable String appointmentId) {
        return ApiResponse.ok(queueService.position(appointmentId));
    }
}
