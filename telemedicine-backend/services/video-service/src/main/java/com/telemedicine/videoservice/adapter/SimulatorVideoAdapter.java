package com.telemedicine.videoservice.adapter;

import com.telemedicine.common.integration.ProviderAdapter;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.client.IntegrationHttpClientFactory;

@ProviderAdapter("simulator")
class SimulatorVideoAdapter extends AbstractSimulatorVideoAdapter {

    SimulatorVideoAdapter(
            IntegrationHttpClientFactory clientFactory,
            TelemedicineIntegrationProperties properties) {
        super(clientFactory, properties, "simulator", "/sim/vonage");
    }
}
