package com.telemedicine.common.integration.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cached read client for platform configuration.
 *
 * <p>Services and BFFs should use this rather than calling config-service directly.
 */
public class PlatformConfigClient {

    private static final Logger log = LoggerFactory.getLogger(PlatformConfigClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Duration cacheTtl;
    private final Cache<String, PlatformConfigSnapshot> cache =
            Caffeine.newBuilder().maximumSize(1).build();

    public PlatformConfigClient(
            RestClient restClient, ObjectMapper objectMapper, Duration cacheTtl) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.cacheTtl = cacheTtl;
    }

    public PlatformConfigSnapshot snapshot() {
        PlatformConfigSnapshot cached = cache.getIfPresent("snapshot");
        if (cached != null && !expired(cached)) {
            return cached;
        }
        String body;
        try {
            body = restClient.get().uri("/api/v1/config").retrieve().body(String.class);
        } catch (RestClientException exception) {
            log.warn("Config service unreachable, using cached or default snapshot", exception);
            if (cached != null) {
                return cached;
            }
            return PlatformConfigSnapshot.defaults();
        }
        PlatformConfigSnapshot parsed = parse(body);
        cache.put("snapshot", parsed);
        return parsed;
    }

    public void invalidate() {
        cache.invalidateAll();
    }

    private boolean expired(PlatformConfigSnapshot snapshot) {
        if (snapshot.refreshedAt() == null) {
            return true;
        }
        return snapshot.refreshedAt().plus(cacheTtl).isBefore(java.time.Instant.now());
    }

    private PlatformConfigSnapshot parse(String body) throws RestClientException {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.get("data") : root;
            String video = text(data, "videoProvider", "simulator");
            String chat = text(data, "chatProvider", "simulator");
            boolean telemetry = data.path("telemetryEnabled").asBoolean(true);
            Map<String, Boolean> channels = new HashMap<>();
            JsonNode notification = data.path("notificationChannels");
            if (notification.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = notification.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    channels.put(entry.getKey(), entry.getValue().asBoolean());
                }
            }
            Map<String, Boolean> mfes = new HashMap<>();
            JsonNode mfeNode = data.path("mfeRemotes");
            if (mfeNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = mfeNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    mfes.put(entry.getKey(), entry.getValue().asBoolean());
                }
            }
            String rules = data.path("telemetryRules").asText("{}");
            return new PlatformConfigSnapshot(
                    video,
                    chat,
                    telemetry,
                    Map.copyOf(channels),
                    Map.copyOf(mfes),
                    rules,
                    java.time.Instant.now());
        } catch (Exception exception) {
            throw new RestClientException("Failed to parse config snapshot", exception);
        }
    }

    private static String text(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? defaultValue : value.asText(defaultValue);
    }
}
