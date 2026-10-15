package com.telemedicine.adminbff.client;

import com.telemedicine.common.core.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DownstreamClient {

    private static final Logger log = LoggerFactory.getLogger(DownstreamClient.class);

    public <T> T getOrEmpty(
            RestClient client, String uri, ParameterizedTypeReference<ApiResponse<T>> type) {
        try {
            ApiResponse<T> response = client.get().uri(uri).retrieve().body(type);
            if (response != null && response.success()) {
                return response.data();
            }
            return null;
        } catch (RestClientException exception) {
            log.warn("Downstream GET {} failed: {}", uri, exception.getMessage());
            return null;
        }
    }

    public <T> T put(
            RestClient client,
            String uri,
            Object body,
            ParameterizedTypeReference<ApiResponse<T>> type) {
        try {
            ApiResponse<T> response =
                    client.put().uri(uri).body(body).retrieve().body(type);
            if (response != null && response.success()) {
                return response.data();
            }
            return null;
        } catch (RestClientException exception) {
            log.warn("Downstream PUT {} failed: {}", uri, exception.getMessage());
            throw exception;
        }
    }
}
