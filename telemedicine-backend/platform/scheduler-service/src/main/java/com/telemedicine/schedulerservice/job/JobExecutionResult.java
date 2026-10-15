package com.telemedicine.schedulerservice.job;

import com.telemedicine.schedulerservice.domain.JobDefinition;

public record JobExecutionResult(boolean failOpen, String message) {

    public static JobExecutionResult success() {
        return new JobExecutionResult(false, "OK");
    }

    public static JobExecutionResult failOpen(String message) {
        return new JobExecutionResult(true, message);
    }

    public static JobExecutionResult failure(String message) {
        return new JobExecutionResult(false, message);
    }

    public com.telemedicine.schedulerservice.domain.JobRun.Status toRunStatus() {
        if (failOpen || "OK".equals(message)) {
            return com.telemedicine.schedulerservice.domain.JobRun.Status.SUCCESS;
        }
        return com.telemedicine.schedulerservice.domain.JobRun.Status.FAILED;
    }
}
