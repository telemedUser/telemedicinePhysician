package com.telemedicine.integrationservice.infrastructure.mock;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.mockdata.MockDatastoreFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Dev-only — delete with {@code infrastructure/mock}. */
@Profile("mock")
@RestController
@RequestMapping("/actuator/mock")
public class MockDataAdminController {
    private final MockDatastoreFactory datastoreFactory;

    public MockDataAdminController(MockDatastoreFactory datastoreFactory) {
        this.datastoreFactory = datastoreFactory;
    }

    @PostMapping("/reset")
    public ApiResponse<Map<String, String>> reset() {
        datastoreFactory.resetAll();
        return ApiResponse.ok(Map.of(
                "status", "reset",
                "message", "All mock datastore connection overlays cleared; seed files unchanged"));
    }
}
