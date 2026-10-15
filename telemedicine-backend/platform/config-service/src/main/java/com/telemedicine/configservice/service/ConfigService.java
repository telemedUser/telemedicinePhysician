package com.telemedicine.configservice.service;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.ConfigurationChanged;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.web.PageResponse;
import com.telemedicine.common.web.page.PageQuery;
import com.telemedicine.common.web.page.SortSpec;
import com.telemedicine.configservice.domain.ConfigRevision;
import com.telemedicine.configservice.domain.PlatformConfig;
import com.telemedicine.configservice.dto.ConfigEntryDto;
import com.telemedicine.configservice.dto.ConfigRevisionDto;
import com.telemedicine.configservice.dto.ConfigSnapshotDto;
import com.telemedicine.configservice.repository.ConfigRevisionRepository;
import com.telemedicine.configservice.repository.PlatformConfigRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigService {

    private static final String GLOBAL_SCOPE = "GLOBAL";

    private final PlatformConfigRepository configRepository;
    private final ConfigRevisionRepository revisionRepository;
    private final OutboxRecorder outboxRecorder;
    private final ConfigSnapshotCache snapshotCache;

    public ConfigService(
            PlatformConfigRepository configRepository,
            ConfigRevisionRepository revisionRepository,
            OutboxRecorder outboxRecorder,
            ConfigSnapshotCache snapshotCache) {
        this.configRepository = configRepository;
        this.revisionRepository = revisionRepository;
        this.outboxRecorder = outboxRecorder;
        this.snapshotCache = snapshotCache;
    }

    public ConfigSnapshotDto getSnapshot() {
        ConfigSnapshotDto cached = snapshotCache.get();
        if (cached != null) {
            return cached;
        }
        ConfigSnapshotDto snapshot = buildSnapshotFromDatabase();
        snapshotCache.put(snapshot);
        return snapshot;
    }

    public ConfigEntryDto getEntry(String key) {
        PlatformConfig config =
                configRepository
                        .findById(key)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.CONFIG_KEY_NOT_FOUND,
                                                "Configuration key not found: " + key));
        return toEntryDto(config);
    }

    @Transactional
    public ConfigEntryDto updateEntry(String key, String value, String changedBy) {
        ConfigKeyValidator.validate(key, value);
        String normalizedValue = normalizeValue(key, value);

        PlatformConfig config =
                configRepository
                        .findById(key)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.CONFIG_KEY_NOT_FOUND,
                                                "Configuration key not found: " + key));

        String previousHash = ConfigValueHasher.hash(config.getConfigValue());
        String newHash = ConfigValueHasher.hash(normalizedValue);

        if (normalizedValue.equals(config.getConfigValue())) {
            return toEntryDto(config);
        }

        Instant changedAt = Instant.now();
        config.setConfigValue(normalizedValue);
        config.setUpdatedAt(changedAt);
        configRepository.save(config);

        ConfigRevision revision = new ConfigRevision();
        revision.setId(IdGenerator.newUlid());
        revision.setConfigKey(key);
        revision.setPreviousValueHash(previousHash);
        revision.setNewValueHash(newHash);
        revision.setChangedBy(changedBy);
        revision.setChangedAt(changedAt);
        revisionRepository.save(revision);

        ConfigurationChanged event =
                new ConfigurationChanged(
                        key, GLOBAL_SCOPE, previousHash, newHash, changedBy, changedAt);
        outboxRecorder.record(
                "PlatformConfig",
                key,
                event,
                MDC.get(MdcKeys.CORRELATION_ID),
                GLOBAL_SCOPE);

        snapshotCache.evict();
        return toEntryDto(config);
    }

    public PageResponse<ConfigRevisionDto> getHistory(String key, PageQuery pageQuery) {
        if (!configRepository.existsById(key)) {
            throw new NotFoundException(
                    ErrorCode.CONFIG_KEY_NOT_FOUND, "Configuration key not found: " + key);
        }
        Page<ConfigRevision> page =
                revisionRepository.findByConfigKey(key, toPageable(pageQuery));
        List<ConfigRevisionDto> items = page.getContent().stream().map(this::toRevisionDto).toList();
        return PageResponse.of(items, pageQuery.page(), pageQuery.size(), page.getTotalElements());
    }

    private ConfigSnapshotDto buildSnapshotFromDatabase() {
        Map<String, String> values = new HashMap<>();
        for (PlatformConfig config : configRepository.findAll()) {
            values.put(config.getConfigKey(), config.getConfigValue());
        }

        Map<String, Boolean> notificationChannels = new LinkedHashMap<>();
        Map<String, Boolean> mfeRemotes = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String configKey = entry.getKey();
            if (configKey.startsWith("notification.") && configKey.endsWith(".enabled")) {
                String channel =
                        configKey.substring("notification.".length(), configKey.length() - ".enabled".length());
                notificationChannels.put(channel, parseBoolean(entry.getValue()));
            } else if (configKey.startsWith("mfe.") && configKey.endsWith(".enabled")) {
                String remote =
                        configKey.substring("mfe.".length(), configKey.length() - ".enabled".length());
                mfeRemotes.put(remote, parseBoolean(entry.getValue()));
            }
        }

        return new ConfigSnapshotDto(
                values.getOrDefault("video.provider", "simulator"),
                values.getOrDefault("chat.provider", "simulator"),
                parseBoolean(values.getOrDefault("telemetry.enabled", "true")),
                Map.copyOf(notificationChannels),
                Map.copyOf(mfeRemotes),
                values.getOrDefault("telemetry.activation.rules", "{}"),
                Instant.now());
    }

    private static String normalizeValue(String key, String value) {
        if (key.endsWith(".enabled")) {
            return Boolean.parseBoolean(value.trim()) ? "true" : "false";
        }
        return value.trim();
    }

    private static boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    private ConfigEntryDto toEntryDto(PlatformConfig config) {
        return new ConfigEntryDto(config.getConfigKey(), config.getConfigValue(), config.getUpdatedAt());
    }

    private ConfigRevisionDto toRevisionDto(ConfigRevision revision) {
        return new ConfigRevisionDto(
                revision.getId(),
                revision.getConfigKey(),
                revision.getPreviousValueHash(),
                revision.getNewValueHash(),
                revision.getChangedBy(),
                revision.getChangedAt());
    }

    private Pageable toPageable(PageQuery pageQuery) {
        if (!pageQuery.isSorted()) {
            return PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.DESC, "changedAt"));
        }
        Sort sort =
                Sort.by(
                        pageQuery.sort().stream()
                                .map(spec -> new Sort.Order(toDirection(spec), spec.field()))
                                .toList());
        return PageRequest.of(pageQuery.page(), pageQuery.size(), sort);
    }

    private static Sort.Direction toDirection(SortSpec spec) {
        return spec.descending() ? Sort.Direction.DESC : Sort.Direction.ASC;
    }
}
