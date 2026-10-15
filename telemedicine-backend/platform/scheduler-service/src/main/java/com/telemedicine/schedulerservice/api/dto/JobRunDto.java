package com.telemedicine.schedulerservice.api.dto;

import com.telemedicine.schedulerservice.domain.JobRun;
import java.time.Instant;

public record JobRunDto(
        String id,
        String jobKey,
        Instant startedAt,
        Instant finishedAt,
        String status,
        String message,
        String triggerType) {

    public static JobRunDto from(JobRun run) {
        return new JobRunDto(
                run.getId(),
                run.getJobKey(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getStatus().name(),
                run.getMessage(),
                run.getTriggerType().name());
    }
}
