package com.telemedicine.schedulerservice.service;

import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.common.core.error.ValidationException;
import com.telemedicine.common.web.PageResponse;
import com.telemedicine.common.web.page.PageQuery;
import com.telemedicine.common.web.page.PageRequestSupport;
import com.telemedicine.common.web.page.SortSpec;
import com.telemedicine.schedulerservice.api.dto.JobDefinitionDto;
import com.telemedicine.schedulerservice.api.dto.JobRunDto;
import com.telemedicine.schedulerservice.api.dto.UpdateJobRequest;
import com.telemedicine.schedulerservice.domain.JobDefinition;
import com.telemedicine.schedulerservice.domain.JobRun;
import com.telemedicine.schedulerservice.job.JobExecutionService;
import com.telemedicine.schedulerservice.repository.JobDefinitionRepository;
import com.telemedicine.schedulerservice.repository.JobRunRepository;
import java.util.List;
import java.util.Set;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    private static final Set<String> RUN_SORT_FIELDS = Set.of("startedAt");

    private final JobDefinitionRepository jobDefinitionRepository;
    private final JobRunRepository jobRunRepository;
    private final QuartzSchedulingService quartzSchedulingService;
    private final JobExecutionService jobExecutionService;
    private final PageRequestSupport pageRequestSupport;

    public SchedulerService(
            JobDefinitionRepository jobDefinitionRepository,
            JobRunRepository jobRunRepository,
            QuartzSchedulingService quartzSchedulingService,
            JobExecutionService jobExecutionService,
            PageRequestSupport pageRequestSupport) {
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.jobRunRepository = jobRunRepository;
        this.quartzSchedulingService = quartzSchedulingService;
        this.jobExecutionService = jobExecutionService;
        this.pageRequestSupport = pageRequestSupport;
    }

    @Transactional(readOnly = true)
    public List<JobDefinitionDto> listJobs() {
        return jobDefinitionRepository.findAllByOrderByJobKeyAsc().stream()
                .map(JobDefinitionDto::from)
                .toList();
    }

    @Transactional
    public JobDefinitionDto updateJob(String key, UpdateJobRequest request) {
        JobDefinition definition =
                jobDefinitionRepository
                        .findById(key)
                        .orElseThrow(() -> NotFoundException.of("SchedulerJob", key));

        if (request.cron() != null) {
            if (!CronExpression.isValidExpression(request.cron())) {
                throw new ValidationException("Invalid cron expression: " + request.cron());
            }
            definition.setCronExpression(request.cron());
        }
        if (request.enabled() != null) {
            definition.setEnabled(request.enabled());
        }

        JobDefinition saved = jobDefinitionRepository.save(definition);
        syncQuartz(saved);
        return JobDefinitionDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobRunDto> listRuns(String jobKey, Integer page, Integer size, String sort) {
        PageQuery query =
                pageRequestSupport.resolve(page, size, sort, RUN_SORT_FIELDS);
        Sort springSort = toSpringSort(query.sort());
        PageRequest pageRequest = PageRequest.of(query.page(), query.size(), springSort);

        Page<JobRun> runs =
                jobKey == null || jobKey.isBlank()
                        ? jobRunRepository.findAllByOrderByStartedAtDesc(pageRequest)
                        : jobRunRepository.findByJobKey(jobKey, pageRequest);

        List<JobRunDto> items = runs.getContent().stream().map(JobRunDto::from).toList();
        return PageResponse.of(items, query.page(), query.size(), runs.getTotalElements());
    }

    public void triggerJob(String key) {
        jobDefinitionRepository
                .findById(key)
                .orElseThrow(() -> NotFoundException.of("SchedulerJob", key));
        jobExecutionService.execute(key, JobRun.TriggerType.MANUAL);
    }

    public void syncAllJobs() {
        jobDefinitionRepository.findAllByOrderByJobKeyAsc().forEach(this::syncQuartz);
    }

    private void syncQuartz(JobDefinition definition) {
        try {
            quartzSchedulingService.sync(definition);
        } catch (SchedulerException exception) {
            log.error("Failed to sync Quartz job {}", definition.getJobKey(), exception);
            throw new ValidationException(
                    "Unable to sync scheduler for job "
                            + definition.getJobKey()
                            + ": "
                            + exception.getMessage());
        }
    }

    private Sort toSpringSort(List<SortSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "startedAt");
        }
        List<Sort.Order> orders =
                specs.stream()
                        .map(
                                spec ->
                                        spec.descending()
                                                ? Sort.Order.desc(spec.field())
                                                : Sort.Order.asc(spec.field()))
                        .toList();
        return Sort.by(orders);
    }
}
