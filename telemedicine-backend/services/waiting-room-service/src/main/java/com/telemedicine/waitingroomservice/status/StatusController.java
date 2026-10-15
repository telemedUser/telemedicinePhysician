package com.telemedicine.waitingroomservice.status;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.core.error.ForbiddenException;
import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.waitingroomservice.domain.WaitingRoomStatus;
import com.telemedicine.waitingroomservice.web.dto.StatusUpdateRequest;
import com.telemedicine.waitingroomservice.web.dto.StatusUpdateResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/waiting-room/status")
public class StatusController {

    private final StatusService statusService;
    private final SecurityContextAccessor securityContext;

    public StatusController(StatusService statusService, SecurityContextAccessor securityContext) {
        this.statusService = statusService;
        this.securityContext = securityContext;
    }

    @PostMapping("/{appointmentId}")
    public ApiResponse<StatusUpdateResponse> updateStatus(
            @PathVariable String appointmentId, @Valid @RequestBody StatusUpdateRequest body) {
        WaitingRoomStatus target = WaitingRoomStatus.fromWire(body.status());
        if (target == WaitingRoomStatus.OPEN_DOCTOR) {
            requirePhysician();
        } else if (target == WaitingRoomStatus.CHECKED_IN) {
            requirePatient();
        } else {
            throw ForbiddenException.missingRole(Roles.REALM_PATIENT);
        }
        return ApiResponse.ok(statusService.update(appointmentId, body));
    }

    private void requirePatient() {
        if (!securityContext.hasRole(Roles.PATIENT)) {
            throw ForbiddenException.missingRole(Roles.REALM_PATIENT);
        }
    }

    private void requirePhysician() {
        if (!securityContext.hasRole(Roles.PHYSICIAN)) {
            throw ForbiddenException.missingRole(Roles.REALM_PHYSICIAN);
        }
    }
}
