package com.telemedicine.common.testing.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.phi.Phi;
import com.telemedicine.common.events.EventTypes;
import com.telemedicine.common.events.TopicNames;
import com.telemedicine.common.events.payload.EventPayload;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reusable contract test asserting {@link EventTypes}, {@code event-catalogue.yaml} and {@code
 * topics.txt} stay in sync.
 *
 * <p>Services extend this class unchanged to inherit the guard without duplicating the logic now
 * living in {@code java-common-events}.
 */
public abstract class EventCatalogueContractTest {

    private static final Pattern TYPE_LINE = Pattern.compile("^\\s*-\\s*type:\\s*(\\S+)\\s*$");
    private static final Pattern PHI_FIELD =
            Pattern.compile(
                    "(?i)(\\b(email|rut|address|mrn|pan|jwt|token|password)\\b|"
                        + "phoneNumber|mobilePhone|patientName|physicianName|firstName|lastName|^name$)");

    @Test
    void eventTypesMatchTheYamlCatalogue() throws IOException {
        assertThat(EventTypes.ALL).containsExactlyInAnyOrderElementsOf(readCatalogueTypes());
    }

    @Test
    void topicsFileListsEveryTopicRetryAndDlq() throws IOException {
        assertThat(readTopicsFile()).containsExactlyInAnyOrderElementsOf(TopicNames.allTopics());
    }

    @Test
    @DisplayName("every catalogue event has a payload record with no obvious PHI field names")
    void payloadRecordsAreCataloguedAndPhiFree() throws ReflectiveOperationException {
        Set<String> covered = new HashSet<>();
        for (String eventType : EventTypes.ALL) {
            Class<?> type = Class.forName("com.telemedicine.common.events.payload." + eventType);
            assertThat(EventPayload.class).isAssignableFrom(type);
            assertThat(type.isRecord()).isTrue();

            EventPayload sample = instantiate(type);
            assertThat(sample.eventType()).isEqualTo(eventType);
            covered.add(eventType);

            for (RecordComponent component : type.getRecordComponents()) {
                assertThat(component.isAnnotationPresent(Phi.class)).isFalse();
                assertThat(PHI_FIELD.matcher(component.getName()).find())
                        .withFailMessage(
                                "Field %s.%s looks like PHI and must not appear on an event"
                                        + " payload",
                                type.getSimpleName(), component.getName())
                        .isFalse();
            }
        }
        assertThat(covered).containsExactlyInAnyOrderElementsOf(EventTypes.ALL);
    }

    private static EventPayload instantiate(Class<?> type) throws ReflectiveOperationException {
        RecordComponent[] components = type.getRecordComponents();
        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            Class<?> argType = components[i].getType();
            if (argType == String.class) {
                args[i] = "sample";
            } else if (argType == int.class || argType == Integer.class) {
                args[i] = 1;
            } else if (argType == long.class || argType == Long.class) {
                args[i] = 1L;
            } else if (argType == boolean.class || argType == Boolean.class) {
                args[i] = true;
            } else if (argType == java.time.Instant.class) {
                args[i] = java.time.Instant.parse("2026-01-01T00:00:00Z");
            } else {
                args[i] = null;
            }
        }
        return (EventPayload) type.getDeclaredConstructor(argTypes(components)).newInstance(args);
    }

    private static Class<?>[] argTypes(RecordComponent[] components) {
        Class<?>[] types = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            types[i] = components[i].getType();
        }
        return types;
    }

    private static List<String> readCatalogueTypes() throws IOException {
        Path contracts = Path.of(System.getProperty("telemedicine.contracts.dir", ""));
        Path catalogue = contracts.resolve("events/event-catalogue.yaml");
        assertThat(catalogue).exists();

        List<String> types = new ArrayList<>();
        for (String line : Files.readAllLines(catalogue)) {
            Matcher matcher = TYPE_LINE.matcher(line);
            if (matcher.matches()) {
                types.add(matcher.group(1));
            }
        }
        return types;
    }

    private static List<String> readTopicsFile() throws IOException {
        Path platform = Path.of(System.getProperty("telemedicine.platform.dir", ""));
        Path topics = platform.resolve("infra/kafka/topics.txt");
        assertThat(topics).exists();
        return Files.readAllLines(topics).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
    }
}
