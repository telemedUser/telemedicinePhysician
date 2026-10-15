package com.telemedicine.physicianservice.chat.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound chat message. {@code senderRole} is validated against the authenticated principal server
 * side; a physician cannot send as the patient.
 */
public record SendChatMessageRequest(@NotBlank String body, String senderRole, Boolean clinical) {}
