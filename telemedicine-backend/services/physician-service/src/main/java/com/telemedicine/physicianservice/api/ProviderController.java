package com.telemedicine.physicianservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.security.annotation.RequiresRole;
import com.telemedicine.physicianservice.api.dto.ProviderDetailResponse;
import com.telemedicine.physicianservice.api.dto.ProviderStatusRequest;
import com.telemedicine.physicianservice.api.dto.ProviderSummaryResponse;
import com.telemedicine.physicianservice.service.ProviderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provider & Physician Management API (PRD §21). The directory and detail are read by the physician
 * app and by provider administrators; status changes take immediate effect on routing (TM-PRV-003).
 * Market is carried by the {@code X-Market} header (TM-MKT-002).
 */
@RestController
@RequestMapping("/api/v1/physicians")
@RequiresRole({"PHYSICIAN", "ADMIN"})
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    public ApiResponse<List<ProviderSummaryResponse>> search(
            @RequestHeader(value = "X-Market", defaultValue = "IN") String market,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String language) {
        return ApiResponse.ok(providerService.search(market, status, specialty, mode, language));
    }

    @GetMapping("/{providerId}")
    public ApiResponse<ProviderDetailResponse> detail(
            @PathVariable String providerId,
            @RequestHeader(value = "X-Market", defaultValue = "IN") String market) {
        return ApiResponse.ok(providerService.detail(providerId, market));
    }

    @PutMapping("/{providerId}/status")
    public ApiResponse<ProviderDetailResponse> changeStatus(
            @PathVariable String providerId,
            @RequestHeader(value = "X-Market", defaultValue = "IN") String market,
            @Valid @RequestBody ProviderStatusRequest request) {
        return ApiResponse.ok(providerService.changeStatus(providerId, market, request));
    }
}
