package com.telemedicine.schedulerservice.job;

import com.telemedicine.schedulerservice.domain.JobDefinition;

public interface JobHandler {

    boolean supports(String jobKey);

    JobExecutionResult execute(JobDefinition definition);
}
