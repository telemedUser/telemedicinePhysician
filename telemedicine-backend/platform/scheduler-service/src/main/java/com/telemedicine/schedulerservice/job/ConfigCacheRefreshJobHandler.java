package com.telemedicine.schedulerservice.job;

import com.telemedicine.schedulerservice.domain.JobDefinition;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Order(0)
public class ConfigCacheRefreshJobHandler implements JobHandler {

    private static final String JOB_KEY = "config-cache-refresh";
    private static final Logger log = LoggerFactory.getLogger(ConfigCacheRefreshJobHandler.class);

    private final Counter tickCounter;

    public ConfigCacheRefreshJobHandler(MeterRegistry meterRegistry) {
        this.tickCounter =
                Counter.builder("telemedicine.scheduler.config_cache_refresh")
                        .description("Config cache refresh scheduler ticks")
                        .register(meterRegistry);
    }

    @Override
    public boolean supports(String jobKey) {
        return JOB_KEY.equals(jobKey);
    }

    @Override
    public JobExecutionResult execute(JobDefinition definition) {
        tickCounter.increment();
        log.debug("Config cache refresh tick recorded for job {}", definition.getJobKey());
        return JobExecutionResult.success();
    }
}
