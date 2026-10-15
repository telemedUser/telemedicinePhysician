package com.telemedicine.physicianservice.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to change a provider's operational status (PRD §21, TM-PRV-003). The change takes
 * immediate effect on routing and the directory.
 *
 * @param status ACTIVE | SUSPENDED | ON_LEAVE | OFFBOARDED
 * @param reason optional audited reason
 */
public record ProviderStatusRequest(@NotBlank String status, String reason) {}
