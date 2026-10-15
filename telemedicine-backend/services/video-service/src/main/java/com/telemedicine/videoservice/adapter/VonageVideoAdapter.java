package com.telemedicine.videoservice.adapter;

import com.telemedicine.common.integration.ProviderAdapter;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;

@ProviderAdapter("vonage")
class VonageVideoAdapter extends AbstractSimulatorVideoAdapter {

    VonageVideoAdapter(
            IntegrationHttpClientFactory clientFactory,
            TelemedicineIntegrationProperties properties) {
        super(clientFactory, properties, "vonage", "/sim/vonage");
    }
}
