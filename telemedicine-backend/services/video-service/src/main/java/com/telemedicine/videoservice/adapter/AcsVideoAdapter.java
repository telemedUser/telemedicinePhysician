package com.telemedicine.videoservice.adapter;

import com.telemedicine.common.integration.ProviderAdapter;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;

@ProviderAdapter("acs")
class AcsVideoAdapter extends AbstractSimulatorVideoAdapter {

    AcsVideoAdapter(
            IntegrationHttpClientFactory clientFactory,
            TelemedicineIntegrationProperties properties) {
        super(clientFactory, properties, "acs", "/sim/acs");
    }
}
