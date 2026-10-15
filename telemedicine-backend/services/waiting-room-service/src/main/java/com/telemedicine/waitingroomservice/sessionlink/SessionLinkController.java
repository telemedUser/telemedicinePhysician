package com.telemedicine.waitingroomservice.sessionlink;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.RequiresRole;
import com.telemedicine.waitingroomservice.web.dto.SessionLinkRequest;
import com.telemedicine.waitingroomservice.web.dto.SessionLinkResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/waiting-room/session-link")
public class SessionLinkController {

    private final SessionLinkService sessionLinkService;

    public SessionLinkController(SessionLinkService sessionLinkService) {
        this.sessionLinkService = sessionLinkService;
    }

    @PostMapping
    @RequiresRole({"SERVICE", "ADMIN"})
    public ApiResponse<SessionLinkResponse> createLink(@Valid @RequestBody SessionLinkRequest body) {
        return ApiResponse.ok(sessionLinkService.create(body));
    }
}
