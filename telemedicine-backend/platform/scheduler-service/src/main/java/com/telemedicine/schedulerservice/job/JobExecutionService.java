package com.telemedicine.schedulerservice.job;

import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.schedulerservice.domain.JobDefinition;
import com.telemedicine.schedulerservice.domain.JobRun;
import com.telemedicine.schedulerservice.repository.JobDefinitionRepository;
import com.telemedicine.schedulerservice.repository.JobRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionService.class);

    private final JobDefinitionRepository jobDefinitionRepository;
    private final JobRunRepository jobRunRepository;
    private final JobHandlerRegistry jobHandlerRegistry;

    public JobExecutionService(
            JobDefinitionRepository jobDefinitionRepository,
            JobRunRepository jobRunRepository,
            JobHandlerRegistry jobHandlerRegistry) {
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.jobRunRepository = jobRunRepository;
        this.jobHandlerRegistry = jobHandlerRegistry;
    }

    @Transactional
    public void execute(String jobKey, JobRun.TriggerType triggerType) {
        JobDefinition definition =
                jobDefinitionRepository
                        .findById(jobKey)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Job definition missing for key " + jobKey));

        if (!definition.isEnabled() && triggerType == JobRun.TriggerType.SCHEDULED) {
            log.debug("Skipping disabled job {}", jobKey);
            return;
        }

        JobRun run =
                new JobRun(
                        IdGenerator.newUlid(),
                        jobKey,
                        java.time.Instant.now(),
                        JobRun.Status.RUNNING,
                        triggerType);
        jobRunRepository.save(run);

        JobExecutionResult result = jobHandlerRegistry.execute(definition);
        JobRun.Status status = result.toRunStatus();
        String message =
                result.failOpen()
                        ? "fail-open: " + result.message()
                        : result.message();
        run.complete(status, message);
        jobRunRepository.save(run);

        log.info("Job {} finished with status {} ({})", jobKey, status, message);
    }
}
