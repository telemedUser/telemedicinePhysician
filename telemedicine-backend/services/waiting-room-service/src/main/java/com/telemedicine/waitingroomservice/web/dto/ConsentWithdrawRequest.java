package com.telemedicine.waitingroomservice.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsentWithdrawRequest(@NotBlank String appointmentId) {}
