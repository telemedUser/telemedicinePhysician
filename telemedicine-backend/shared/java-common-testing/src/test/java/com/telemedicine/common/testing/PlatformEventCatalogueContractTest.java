package com.telemedicine.common.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.payload.ConfigurationChanged;
import com.telemedicine.common.testing.contract.EventCatalogueContractTest;
import java.time.Instant;

/** Ensures the shared catalogue contract base stays wired to the Phase 0 artefacts. */
class PlatformEventCatalogueContractTest extends EventCatalogueContractTest {

    @org.junit.jupiter.api.Test
    void smokeLoadsCatalogueConstants() {
        assertThat(EventTypes.ALL).isNotEmpty();
        assertThat(
                        new ConfigurationChanged("k", "GLOBAL", null, "v", "admin", Instant.EPOCH)
                                .eventType())
                .isEqualTo(EventTypes.CONFIGURATION_CHANGED);
    }
}
