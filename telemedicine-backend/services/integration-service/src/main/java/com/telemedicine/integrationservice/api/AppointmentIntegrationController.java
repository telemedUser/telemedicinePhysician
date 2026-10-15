package com.telemedicine.integrationservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.integrationservice.domain.Appointment;
import com.telemedicine.integrationservice.domain.MpiPatient;
import com.telemedicine.integrationservice.service.AppointmentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/appointments/v2")
public class AppointmentIntegrationController {
    private final AppointmentService appointmentService;

    public AppointmentIntegrationController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ApiResponse<Appointment> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(appointmentService.create(body));
    }

    @GetMapping("/{id}")
    public ApiResponse<Appointment> consult(@PathVariable String id) {
        return ApiResponse.ok(appointmentService.consult(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Appointment> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(appointmentService.updateStatus(id, body.get("status")));
    }

    @PostMapping("/{id}/reschedule")
    public ApiResponse<Appointment> reschedule(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(appointmentService.reschedule(id, body.get("newSlot")));
    }

    @PostMapping("/{id}/payment/validate")
    public ApiResponse<Appointment> validatePayment(@PathVariable String id) {
        return ApiResponse.ok(appointmentService.validatePayment(id));
    }

    @GetMapping("/{id}/mpi")
    public ApiResponse<MpiPatient> mpiLookup(@PathVariable String id) {
        return ApiResponse.ok(appointmentService.mpiLookup(id));
    }
}
