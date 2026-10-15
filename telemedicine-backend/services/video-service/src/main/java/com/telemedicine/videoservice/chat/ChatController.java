package com.telemedicine.videoservice.chat;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.videoservice.service.VideoChatService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/video/sessions/{sessionId}/chat")
public class ChatController {

    private final VideoChatService chatService;

    public ChatController(VideoChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable String sessionId) {
        return ApiResponse.ok(chatService.listMessages(sessionId));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> send(
            @PathVariable String sessionId, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                chatService.sendMessage(
                        sessionId,
                        body.getOrDefault("senderRole", "PATIENT"),
                        body.get("body")));
    }
}
