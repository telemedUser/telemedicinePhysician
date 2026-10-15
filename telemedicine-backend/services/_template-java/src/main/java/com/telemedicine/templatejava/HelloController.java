package com.telemedicine.templatejava;

import com.telemedicine.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public ApiResponse<Map<String, String>> hello() {
        return ApiResponse.ok(Map.of(
                "service", "template-java",
                "message", "Hello from Java microservice"
        ));
    }
}
