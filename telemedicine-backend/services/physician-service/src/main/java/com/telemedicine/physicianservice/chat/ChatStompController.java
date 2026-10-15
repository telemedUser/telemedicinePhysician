package com.telemedicine.physicianservice.chat;

import com.telemedicine.physicianservice.chat.dto.SendChatMessageRequest;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * STOMP entry point for real-time clinical chat. A client publishes to
 * {@code /app/consultation/{appointmentId}/send}; {@link ChatService} persists the message and
 * fans it out to {@code /topic/consultation/{appointmentId}}, which both parties subscribe to.
 *
 * <p>The sender role is derived from the authenticated STOMP session principal, so a physician
 * session cannot post as the patient. When no principal is present (dev), it defaults to PHYSICIAN.
 */
@Controller
public class ChatStompController {

    private final ChatService chatService;

    public ChatStompController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/consultation/{appointmentId}/send")
    public void onMessage(
            @DestinationVariable String appointmentId,
            SendChatMessageRequest request,
            Principal principal) {
        String senderRef = principal == null ? "anonymous" : principal.getName();
        String role = resolveRole(principal, request);
        chatService.send(appointmentId, role, senderRef, request);
    }

    private String resolveRole(Principal principal, SendChatMessageRequest request) {
        if (principal instanceof org.springframework.security.core.Authentication auth) {
            boolean physician =
                    auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_PHYSICIAN"));
            if (physician) {
                return "PHYSICIAN";
            }
            boolean patient =
                    auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
            if (patient) {
                return "PATIENT";
            }
        }
        // Fallback for unauthenticated dev sessions: trust the declared role, defaulting to physician.
        return request.senderRole() == null ? "PHYSICIAN" : request.senderRole();
    }
}
