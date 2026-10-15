package com.telemedicine.schedulerservice.service;

import com.telemedicine.schedulerservice.domain.JobDefinition;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;
import com.telemedicine.schedulerservice.job.TelemedicineQuartzJob;

@Service
public class QuartzSchedulingService {

    private final Scheduler scheduler;

    public QuartzSchedulingService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void sync(JobDefinition definition) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(definition.getJobKey());
        TriggerKey triggerKey = TriggerKey.triggerKey(definition.getJobKey());

        if (!definition.isEnabled()) {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            return;
        }

        JobDetail jobDetail =
                JobBuilder.newJob(TelemedicineQuartzJob.class)
                        .withIdentity(jobKey)
                        .usingJobData("jobKey", definition.getJobKey())
                        .storeDurably(false)
                        .build();

        Trigger trigger =
                TriggerBuilder.newTrigger()
                        .withIdentity(triggerKey)
                        .forJob(jobKey)
                        .withSchedule(
                                CronScheduleBuilder.cronSchedule(definition.getCronExpression())
                                        .withMisfireHandlingInstructionFireAndProceed())
                        .build();

        if (scheduler.checkExists(jobKey)) {
            scheduler.addJob(jobDetail, true, true);
            scheduler.rescheduleJob(triggerKey, trigger);
        } else {
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }
}
