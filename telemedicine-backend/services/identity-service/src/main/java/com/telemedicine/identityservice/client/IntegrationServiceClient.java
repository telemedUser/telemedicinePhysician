package com.telemedicine.identityservice.client;

import com.telemedicine.common.core.dto.ApiResponse;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.IntegrationException;
import com.telemedicine.common.core.error.NotFoundException;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class IntegrationServiceClient {

    private static final ParameterizedTypeReference<ApiResponse<MpiPatientResponse>> MPI_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<ApiResponse<FonasaChallengeResponse>> CHALLENGE_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<ApiResponse<FonasaVerifyResponse>> VERIFY_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public IntegrationServiceClient(RestClient integrationServiceRestClient) {
        this.restClient = integrationServiceRestClient;
    }

    public MpiPatientResponse mpiLookup(String appointmentId) {
        try {
            ApiResponse<MpiPatientResponse> response =
                    restClient
                            .get()
                            .uri("/v1/appointments/v2/{id}/mpi", appointmentId)
                            .retrieve()
                            .onStatus(
                                    HttpStatusCode::is4xxClientError,
                                    (request, clientResponse) -> {
                                        if (clientResponse.getStatusCode().value() == 404) {
                                            throw new NotFoundException(
                                                    ErrorCode.MPI_PATIENT_NOT_FOUND,
                                                    "MPI patient not found for appointment");
                                        }
                                        throw new IntegrationException(
                                                ErrorCode.INTEGRATION_ERROR,
                                                "integration",
                                                "MPI lookup failed");
                                    })
                            .body(MPI_TYPE);
            if (response == null || response.data() == null) {
                throw new NotFoundException(
                        ErrorCode.MPI_PATIENT_NOT_FOUND, "MPI patient not found for appointment");
            }
            return response.data();
        } catch (NotFoundException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw mapException(exception);
        } catch (RestClientException exception) {
            throw new IntegrationException(
                    ErrorCode.INTEGRATION_ERROR, "integration", exception.getMessage(), exception);
        }
    }

    public FonasaChallengeResponse fonasaChallenge(
            String appointmentId, String patientId, String rut, String phone) {
        try {
            ApiResponse<FonasaChallengeResponse> response =
                    restClient
                            .post()
                            .uri("/api/v1/integration/fonasa/2fa/challenge")
                            .body(Map.of(
                                    "appointmentId", appointmentId,
                                    "patientId", patientId,
                                    "rut", rut != null ? rut : "",
                                    "phone", phone != null ? phone : ""))
                            .retrieve()
                            .body(CHALLENGE_TYPE);
            if (response == null || response.data() == null || response.data().challengeId() == null) {
                throw new IntegrationException(
                        ErrorCode.INTEGRATION_ERROR, "fonasa", "FONASA challenge response was empty");
            }
            return response.data();
        } catch (RestClientResponseException exception) {
            throw mapException(exception);
        } catch (RestClientException exception) {
            throw new IntegrationException(
                    ErrorCode.INTEGRATION_ERROR, "fonasa", exception.getMessage(), exception);
        }
    }

    public FonasaVerifyResponse fonasaVerify(String challengeId, String code, String patientId) {
        try {
            ApiResponse<FonasaVerifyResponse> response =
                    restClient
                            .post()
                            .uri("/api/v1/integration/fonasa/2fa/verify")
                            .body(Map.of(
                                    "challengeId", challengeId,
                                    "code", code,
                                    "patientId", patientId))
                            .retrieve()
                            .body(VERIFY_TYPE);
            if (response == null || response.data() == null) {
                throw new IntegrationException(
                        ErrorCode.INTEGRATION_ERROR, "fonasa", "FONASA verify response was empty");
            }
            return response.data();
        } catch (RestClientResponseException exception) {
            throw mapException(exception);
        } catch (RestClientException exception) {
            throw new IntegrationException(
                    ErrorCode.INTEGRATION_ERROR, "fonasa", exception.getMessage(), exception);
        }
    }

    private static IntegrationException mapException(RestClientResponseException exception) {
        return new IntegrationException(
                ErrorCode.INTEGRATION_ERROR,
                "integration",
                "Integration service call failed",
                exception);
    }

    public record MpiPatientResponse(
            String appointmentId, String patientId, String name, String dateOfBirth) {}

    public record FonasaChallengeResponse(String challengeId, String maskedPhone) {}

    public record FonasaVerifyResponse(boolean verified, String patientId) {}
}
