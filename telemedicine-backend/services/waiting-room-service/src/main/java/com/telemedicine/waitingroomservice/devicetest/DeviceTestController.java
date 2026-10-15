package com.telemedicine.waitingroomservice.devicetest;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPatient;
import com.telemedicine.waitingroomservice.web.dto.DeviceTestRequest;
import com.telemedicine.waitingroomservice.web.dto.DeviceTestResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/waiting-room/device-test")
public class DeviceTestController {

    private final DeviceTestService deviceTestService;

    public DeviceTestController(DeviceTestService deviceTestService) {
        this.deviceTestService = deviceTestService;
    }

    @PostMapping("/{appointmentId}")
    @IsPatient
    public ApiResponse<DeviceTestResponse> submit(
            @PathVariable String appointmentId, @RequestBody DeviceTestRequest results) {
        return ApiResponse.ok(deviceTestService.submit(appointmentId, results));
    }
}
