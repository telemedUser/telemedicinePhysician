package com.telemedicine.physicianbff.service;

import com.telemedicine.physicianbff.client.DownstreamClient;
import com.telemedicine.physicianbff.client.DownstreamClient.FetchResult;
import com.telemedicine.physicianbff.dto.downstream.ClinicalDocumentResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PhysicianDocumentationService {

    private static final ParameterizedTypeReference<
                    com.telemedicine.common.core.dto.ApiResponse<ClinicalDocumentResponse>>
            DOCUMENT_TYPE = new ParameterizedTypeReference<>() {};

    private final DownstreamClient downstreamClient;
    private final RestClient clinicalDocsRestClient;

    public PhysicianDocumentationService(
            DownstreamClient downstreamClient,
            @Qualifier("clinicalDocsRestClient") RestClient clinicalDocsRestClient) {
        this.downstreamClient = downstreamClient;
        this.clinicalDocsRestClient = clinicalDocsRestClient;
    }

    public ClinicalDocumentResponse submitDocumentation(
            String appointmentId, Map<String, Object> body) {
        Map<String, String> payload = new HashMap<>();
        payload.put("appointmentId", appointmentId);
        if (body != null) {
            body.forEach(
                    (key, value) -> {
                        if (value != null) {
                            payload.put(key, String.valueOf(value));
                        }
                    });
        }

        FetchResult<ClinicalDocumentResponse> result =
                downstreamClient.post(
                        clinicalDocsRestClient, "/api/v1/clinical-docs", payload, DOCUMENT_TYPE);

        if (result.ok() && result.data() != null) {
            return result.data();
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "Clinical documentation submission failed");
    }
}
