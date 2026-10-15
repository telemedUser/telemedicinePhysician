package com.telemedicine.videoservice.adapter;

import com.telemedicine.common.integration.ProviderAdapter;
import com.telemedicine.common.integration.ProviderCategory;

@ProviderAdapter(value = "vonage", category = ProviderCategory.CHAT)
class VonageChatAdapter implements ChatProviderAdapter {
    @Override
    public String providerName() {
        return "vonage";
    }
}

@ProviderAdapter(value = "acs", category = ProviderCategory.CHAT)
class AcsChatAdapter implements ChatProviderAdapter {
    @Override
    public String providerName() {
        return "acs";
    }
}

@ProviderAdapter(value = "simulator", category = ProviderCategory.CHAT)
class SimulatorChatAdapter implements ChatProviderAdapter {
    @Override
    public String providerName() {
        return "simulator";
    }
}
