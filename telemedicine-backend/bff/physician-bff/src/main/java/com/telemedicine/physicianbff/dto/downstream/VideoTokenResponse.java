package com.telemedicine.physicianbff.dto.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VideoTokenResponse(
        String tokenId, String sessionId, String provider, String token, String role) {}
