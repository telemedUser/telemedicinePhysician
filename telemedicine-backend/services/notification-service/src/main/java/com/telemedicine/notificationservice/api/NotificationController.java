package com.telemedicine.notificationservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsService;
import com.telemedicine.common.security.annotation.RequiresRole;
import com.telemedicine.notificationservice.api.dto.AppointmentReminderJobResponse;
import com.telemedicine.notificationservice.api.dto.SendNotificationRequest;
import com.telemedicine.notificationservice.api.dto.SendNotificationResponse;
import com.telemedicine.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    @RequiresRole({"SERVICE", "ADMIN"})
    public ApiResponse<SendNotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        return ApiResponse.ok(notificationService.send(request));
    }
}

@RestController
@RequestMapping("/api/v1/internal/jobs")
class NotificationInternalJobController {

    private final NotificationService notificationService;

    NotificationInternalJobController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/appointment-reminders/run")
    @IsService
    public ApiResponse<AppointmentReminderJobResponse> runAppointmentReminders() {
        return ApiResponse.ok(notificationService.runAppointmentReminders());
    }
}
