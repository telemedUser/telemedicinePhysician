package com.telemedicine.schedulerservice.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class TelemedicineQuartzJob extends QuartzJobBean {

    private final JobExecutionService jobExecutionService;

    public TelemedicineQuartzJob(JobExecutionService jobExecutionService) {
        this.jobExecutionService = jobExecutionService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String jobKey = context.getMergedJobDataMap().getString("jobKey");
        boolean manual = context.getMergedJobDataMap().getBooleanValue("manual");
        JobRunTriggerType triggerType =
                manual ? JobRunTriggerType.MANUAL : JobRunTriggerType.SCHEDULED;
        jobExecutionService.execute(jobKey, triggerType.toDomain());
    }

    private enum JobRunTriggerType {
        SCHEDULED,
        MANUAL;

        com.telemedicine.schedulerservice.domain.JobRun.TriggerType toDomain() {
            return com.telemedicine.schedulerservice.domain.JobRun.TriggerType.valueOf(name());
        }
    }
}
