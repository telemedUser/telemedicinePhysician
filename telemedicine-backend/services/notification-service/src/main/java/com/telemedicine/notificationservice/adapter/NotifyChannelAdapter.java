package com.telemedicine.notificationservice.adapter;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.ProviderOutageException;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class NotifyChannelAdapter {

    private static final ParameterizedTypeReference<Map<String, String>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient notifyClient;

    public NotifyChannelAdapter(RestClient notifyClient) {
        this.notifyClient = notifyClient;
    }

    public SendResult send(String channel, String recipientRef, String body, String subject) {
        try {
            Map<String, String> response =
                    notifyClient
                            .post()
                            .uri("/sim/notify/{channel}", channel.toLowerCase())
                            .body(
                                    Map.of(
                                            "recipientRef", recipientRef,
                                            "body", body,
                                            "subject", subject != null ? subject : ""))
                            .retrieve()
                            .onStatus(
                                    HttpStatusCode::isError,
                                    (request, clientResponse) -> {
                                        throw new ProviderOutageException(
                                                ErrorCode.NOTIFICATION_CHANNEL_UNAVAILABLE,
                                                channel,
                                                "Notification channel returned an error");
                                    })
                            .body(RESPONSE_TYPE);
            String messageId = response != null ? response.get("messageId") : null;
            return new SendResult(true, messageId, null);
        } catch (ProviderOutageException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ProviderOutageException(
                    ErrorCode.NOTIFICATION_CHANNEL_UNAVAILABLE,
                    channel,
                    exception.getMessage(),
                    exception);
        }
    }

    public record SendResult(boolean success, String messageId, String reasonCode) {}
}
