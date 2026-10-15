package com.telemedicine.physicianbff.service;

import com.telemedicine.physicianbff.client.DownstreamClient;
import com.telemedicine.physicianbff.client.DownstreamClient.FetchResult;
import com.telemedicine.physicianbff.dto.AggregationStatus;
import com.telemedicine.physicianbff.dto.WorklistEntry;
import com.telemedicine.physicianbff.dto.WorklistResponse;
import com.telemedicine.physicianbff.dto.downstream.MonitoringSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PhysicianWorklistService {

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<MonitoringSnapshot>>
            SNAPSHOT_TYPE = new ParameterizedTypeReference<>() {};

    private final DownstreamClient downstreamClient;
    private final RestClient monitoringRestClient;

    public PhysicianWorklistService(
            DownstreamClient downstreamClient,
            @Qualifier("monitoringRestClient") RestClient monitoringRestClient) {
        this.downstreamClient = downstreamClient;
        this.monitoringRestClient = monitoringRestClient;
    }

    public WorklistResponse worklist() {
        FetchResult<MonitoringSnapshot> snapshot =
                downstreamClient.get(
                        monitoringRestClient, "/api/v1/monitoring/snapshot", SNAPSHOT_TYPE);

        if (snapshot.ok() && snapshot.data() != null) {
            List<WorklistEntry> entries = new ArrayList<>();
            int queueSize = snapshot.data().waitingRoomQueue();
            for (int index = 0; index < queueSize; index++) {
                entries.add(
                        new WorklistEntry(
                                "apt-queue-" + (index + 1),
                                "CHECKED_IN",
                                "pat-queue-" + (index + 1),
                                AggregationStatus.OK));
            }
            return new WorklistResponse(AggregationStatus.OK, entries);
        }

        return new WorklistResponse(
                AggregationStatus.DEGRADED,
                List.of(
                        new WorklistEntry(
                                "apt-degraded",
                                "UNKNOWN",
                                "pat-unknown",
                                AggregationStatus.DEGRADED)));
    }
}
