package com.telemedicine.simulators.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemedicine.simulator")
public class SimulatorProperties {

    private Map<String, VendorConfig> vendors = defaultVendors();

    public Map<String, VendorConfig> getVendors() {
        return vendors;
    }

    public void setVendors(Map<String, VendorConfig> vendors) {
        this.vendors = vendors != null ? new LinkedHashMap<>(vendors) : defaultVendors();
    }

    public static Map<String, VendorConfig> defaultVendors() {
        Map<String, VendorConfig> defaults = new LinkedHashMap<>();
        for (String vendor : VendorNames.ALL) {
            defaults.put(vendor, VendorConfig.defaults());
        }
        return defaults;
    }
}
