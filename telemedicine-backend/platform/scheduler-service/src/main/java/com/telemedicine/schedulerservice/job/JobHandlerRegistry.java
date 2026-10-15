package com.telemedicine.schedulerservice.job;

import com.telemedicine.schedulerservice.domain.JobDefinition;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JobHandlerRegistry {

    private final List<JobHandler> handlers;

    public JobHandlerRegistry(List<JobHandler> handlers) {
        this.handlers = handlers;
    }

    public JobExecutionResult execute(JobDefinition definition) {
        return handlers.stream()
                .filter(handler -> handler.supports(definition.getJobKey()))
                .findFirst()
                .map(handler -> handler.execute(definition))
                .orElse(JobExecutionResult.failure("No handler registered for job " + definition.getJobKey()));
    }
}
