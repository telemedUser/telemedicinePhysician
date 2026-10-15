package com.telemedicine.physicianservice.chat.dto;

/**
 * A chat message as seen on the wire (sent over STOMP and returned by the REST history endpoint).
 *
 * @param id message id
 * @param appointmentId consultation the message belongs to
 * @param senderRole PHYSICIAN | PATIENT
 * @param body message text (clinical data; stays within the authenticated chat channel)
 * @param clinical whether this is clinical chat vs support chat
 * @param sentAt ISO-8601 timestamp
 */
public record ChatMessageDto(
        String id,
        String appointmentId,
        String senderRole,
        String body,
        boolean clinical,
        String sentAt) {}
