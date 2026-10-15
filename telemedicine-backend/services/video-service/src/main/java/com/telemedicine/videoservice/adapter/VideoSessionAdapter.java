package com.telemedicine.videoservice.adapter;

public interface VideoSessionAdapter {

    String providerName();

    ProviderSession createSession(String appointmentId);

    ProviderToken issueToken(String providerSessionId, String role);
}
