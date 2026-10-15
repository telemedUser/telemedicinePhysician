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
@Order(100)
public class RemoteJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(RemoteJobHandler.class);

    private final RestClient restClient;
    private final Counter downstreamFailureCounter;

    public RemoteJobHandler(RestClient.Builder restClientBuilder, MeterRegistry meterRegistry) {
        this.restClient = restClientBuilder.build();
        this.downstreamFailureCounter =
                Counter.builder("telemedicine.scheduler.downstream_failures")
                        .description("Scheduler downstream call failures handled fail-open")
                        .register(meterRegistry);
    }

    @Override
    public boolean supports(String jobKey) {
        return !"config-cache-refresh".equals(jobKey);
    }

    @Override
    public JobExecutionResult execute(JobDefinition definition) {
        if (definition.getTargetService() == null || definition.getTargetPath() == null) {
            return JobExecutionResult.failure("Job has no downstream target configured");
        }

        String baseUrl = "http://" + definition.getTargetService();
        try {
            restClient
                    .post()
                    .uri(baseUrl + definition.getTargetPath())
                    .retrieve()
                    .toBodilessEntity();
            return JobExecutionResult.success();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return recordFailOpen(definition, "downstream endpoint not found (404)");
            }
            return recordFailOpen(definition, exception.getMessage());
        } catch (RestClientException exception) {
            return recordFailOpen(definition, exception.getMessage());
        }
    }

    private JobExecutionResult recordFailOpen(JobDefinition definition, String reason) {
        downstreamFailureCounter.increment();
        log.warn(
                "Job {} fail-open: downstream {} {} unavailable: {}",
                definition.getJobKey(),
                definition.getTargetService(),
                definition.getTargetPath(),
                reason);
        return JobExecutionResult.failOpen(reason);
    }
}
