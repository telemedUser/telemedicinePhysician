package com.telemedicine.videoservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.IsPhysician;
import com.telemedicine.common.security.annotation.IsService;
import com.telemedicine.videoservice.service.SessionExpiryJobService;
import com.telemedicine.videoservice.service.VideoSessionService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/video")
public class VideoSessionController {

    private final VideoSessionService sessionService;
    private final SessionExpiryJobService expiryJobService;

    public VideoSessionController(
            VideoSessionService sessionService, SessionExpiryJobService expiryJobService) {
        this.sessionService = sessionService;
        this.expiryJobService = expiryJobService;
    }

    @PostMapping("/sessions")
    @IsService
    public ApiResponse<Map<String, String>> createSession(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                sessionService.createSession(
                        body.get("appointmentId"), body.get("recordingPolicy")));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<Map<String, Object>> getSession(@PathVariable String sessionId) {
        return ApiResponse.ok(sessionService.getSession(sessionId));
    }

    @GetMapping("/appointments/{appointmentId}/session")
    public ApiResponse<Map<String, Object>> getSessionByAppointment(
            @PathVariable String appointmentId) {
        return ApiResponse.ok(sessionService.getSessionByAppointment(appointmentId));
    }

    @PostMapping("/sessions/{sessionId}/tokens")
    public ApiResponse<Map<String, String>> issueToken(
            @PathVariable String sessionId, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(sessionService.issueToken(sessionId, body.get("role")));
    }

    @PostMapping("/sessions/{sessionId}/join")
    public ApiResponse<Map<String, Object>> join(
            @PathVariable String sessionId, @RequestBody(required = false) Map<String, String> body) {
        String role = body != null ? body.getOrDefault("role", "PATIENT") : "PATIENT";
        return ApiResponse.ok(sessionService.joinSession(sessionId, role));
    }

    @PostMapping("/sessions/{sessionId}/leave")
    public ApiResponse<Map<String, Object>> leave(
            @PathVariable String sessionId, @RequestBody(required = false) Map<String, String> body) {
        String endReason = body != null ? body.get("endReason") : "COMPLETED";
        return ApiResponse.ok(sessionService.leaveSession(sessionId, endReason));
    }

    @PostMapping("/sessions/{sessionId}/quality")
    @IsPhysician
    public ApiResponse<Map<String, String>> quality(
            @PathVariable String sessionId, @RequestBody Map<String, Object> sample) {
        sessionService.recordQuality(sessionId, sample);
        return ApiResponse.ok(Map.of("sessionId", sessionId, "recorded", "true"));
    }

    @PostMapping("/internal/jobs/session-expiry-cleanup/run")
    @IsService
    public ApiResponse<Map<String, Object>> runExpiryCleanup() {
        int ended = expiryJobService.runCleanup();
        return ApiResponse.ok(Map.of("endedSessions", ended));
    }
}
