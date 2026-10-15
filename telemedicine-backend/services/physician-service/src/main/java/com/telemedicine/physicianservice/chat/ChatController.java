package com.telemedicine.physicianservice.chat;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.common.security.annotation.IsPhysician;
import com.telemedicine.physicianservice.chat.dto.ChatMessageDto;
import com.telemedicine.physicianservice.chat.dto.SendChatMessageRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for clinical chat (PRD §13.3): fetch the transcript and post a message. Real-time
 * delivery is over STOMP (see {@link ChatStompController}); this REST path is the durable, auditable
 * entry point and the history source. Scoped to the physician role.
 */
@RestController
@RequestMapping("/api/v1/physicians/me/consultations/{appointmentId}/chat")
@IsPhysician
public class ChatController {

    private final ChatService chatService;
    private final SecurityContextAccessor securityContext;

    public ChatController(ChatService chatService, SecurityContextAccessor securityContext) {
        this.chatService = chatService;
        this.securityContext = securityContext;
    }

    @GetMapping
    public ApiResponse<List<ChatMessageDto>> history(@PathVariable String appointmentId) {
        return ApiResponse.ok(chatService.history(appointmentId));
    }

    @PostMapping
    public ApiResponse<ChatMessageDto> send(
            @PathVariable String appointmentId, @Valid @RequestBody SendChatMessageRequest request) {
        String senderRef = securityContext.currentUser().actorId();
        // The physician endpoint always records the physician as the sender, regardless of body.
        return ApiResponse.ok(chatService.send(appointmentId, Roles.REALM_PHYSICIAN, senderRef, request));
    }
}
