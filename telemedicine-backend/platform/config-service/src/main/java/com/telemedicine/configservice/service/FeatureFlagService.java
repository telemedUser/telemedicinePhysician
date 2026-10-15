package com.telemedicine.configservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.common.core.error.ValidationException;
import com.telemedicine.configservice.domain.FeatureFlag;
import com.telemedicine.configservice.dto.FeatureFlagDto;
import com.telemedicine.configservice.dto.UpdateFeatureFlagRequest;
import com.telemedicine.configservice.repository.FeatureFlagRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureFlagService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FeatureFlagRepository featureFlagRepository;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    public FeatureFlagDto getFlag(String flagKey) {
        return toDto(findFlag(flagKey));
    }

    @Transactional
    public FeatureFlagDto upsertFlag(String flagKey, UpdateFeatureFlagRequest request) {
        validatePayload(request.payloadJson());
        FeatureFlag flag =
                featureFlagRepository
                        .findById(flagKey)
                        .orElseGet(
                                () -> {
                                    FeatureFlag created = new FeatureFlag();
                                    created.setFlagKey(flagKey);
                                    return created;
                                });
        flag.setEnabled(request.enabled());
        flag.setPayloadJson(request.payloadJson());
        flag.setUpdatedAt(Instant.now());
        return toDto(featureFlagRepository.save(flag));
    }

    private FeatureFlag findFlag(String flagKey) {
        return featureFlagRepository
                .findById(flagKey)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.CONFIG_KEY_NOT_FOUND,
                                        "Feature flag not found: " + flagKey));
    }

    private static void validatePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return;
        }
        try {
            JsonNode node = MAPPER.readTree(payloadJson);
            if (!node.isObject() && !node.isArray()) {
                throw ValidationException.field("payloadJson", "must be a JSON object or array");
            }
        } catch (ValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ValidationException.field("payloadJson", "must be valid JSON");
        }
    }

    private FeatureFlagDto toDto(FeatureFlag flag) {
        return new FeatureFlagDto(
                flag.getFlagKey(), flag.isEnabled(), flag.getPayloadJson(), flag.getUpdatedAt());
    }
}
