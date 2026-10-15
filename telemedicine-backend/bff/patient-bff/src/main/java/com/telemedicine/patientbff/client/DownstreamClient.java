package com.telemedicine.patientbff.client;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.patientbff.dto.AggregationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DownstreamClient {

    private static final Logger log = LoggerFactory.getLogger(DownstreamClient.class);

    public <T> FetchResult<T> get(
            RestClient client,
            String uri,
            Object[] uriVariables,
            ParameterizedTypeReference<ApiResponse<T>> type) {
        try {
            ApiResponse<T> response =
                    client.get().uri(uri, uriVariables).retrieve().body(type);
            if (response != null && response.success() && response.data() != null) {
                return FetchResult.ok(response.data());
            }
            return FetchResult.degraded("Empty or unsuccessful downstream response");
        } catch (RestClientException exception) {
            log.debug("Downstream call failed for {}: {}", uri, exception.getMessage());
            return FetchResult.degraded(exception.getMessage());
        }
    }

    public <T> FetchResult<T> get(
            RestClient client, String uri, ParameterizedTypeReference<ApiResponse<T>> type) {
        return get(client, uri, new Object[] {}, type);
    }

    public record FetchResult<T>(T data, AggregationStatus status, String error) {
        public static <T> FetchResult<T> ok(T data) {
            return new FetchResult<>(data, AggregationStatus.OK, null);
        }

        public static <T> FetchResult<T> degraded(String error) {
            return new FetchResult<>(null, AggregationStatus.DEGRADED, error);
        }

        public boolean ok() {
            return status == AggregationStatus.OK;
        }
    }
}
