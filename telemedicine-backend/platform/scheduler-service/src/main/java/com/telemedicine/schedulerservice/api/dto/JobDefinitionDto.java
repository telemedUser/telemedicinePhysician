package com.telemedicine.schedulerservice.api.dto;

import com.telemedicine.schedulerservice.domain.JobDefinition;
import java.time.Instant;

public record JobDefinitionDto(
        String key,
        String description,
        String cron,
        boolean enabled,
        String targetService,
        String targetPath,
        Instant updatedAt) {

    public static JobDefinitionDto from(JobDefinition definition) {
        return new JobDefinitionDto(
                definition.getJobKey(),
                definition.getDescription(),
                definition.getCronExpression(),
                definition.isEnabled(),
                definition.getTargetService(),
                definition.getTargetPath(),
                definition.getUpdatedAt());
    }
}
