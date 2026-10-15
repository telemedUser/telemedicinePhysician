package com.telemedicine.adminbff.service;

import com.telemedicine.adminbff.dto.AdminConfigView;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.integration.config.PlatformConfigSnapshot;
import org.springframework.stereotype.Service;

@Service
public class AdminConfigService {

    private final PlatformConfigClient platformConfigClient;

    public AdminConfigService(PlatformConfigClient platformConfigClient) {
        this.platformConfigClient = platformConfigClient;
    }

    public AdminConfigView currentConfig() {
        PlatformConfigSnapshot snapshot = platformConfigClient.snapshot();
        return new AdminConfigView(
                snapshot.videoProvider(),
                snapshot.chatProvider(),
                snapshot.telemetryEnabled(),
                snapshot.notificationChannels(),
                snapshot.mfeRemotes(),
                snapshot.telemetryRulesJson());
    }
}
