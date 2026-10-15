package com.telemedicine.common.core.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdGeneratorTest {

    @Test
    void newIdIsAUuidV4() {
        UUID parsed = UUID.fromString(IdGenerator.newId());

        assertThat(parsed.version()).isEqualTo(4);
    }

    @Test
    void newUlidIsAValidUlid() {
        assertThat(Ulid.isValid(IdGenerator.newUlid())).isTrue();
    }

    @Test
    @DisplayName("UUIDv7 sets the version and variant bits required by RFC 9562")
    void uuidV7HasCorrectVersionAndVariant() {
        UUID uuid = IdGenerator.uuidV7();

        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
        assertThat(UUID.fromString(IdGenerator.newUuidV7()).version()).isEqualTo(7);
    }

    @Test
    @DisplayName("UUIDv7 embeds the current millisecond so keys append rather than scatter")
    void uuidV7EmbedsTheTimestamp() {
        long before = System.currentTimeMillis();
        UUID uuid = IdGenerator.uuidV7();
        long after = System.currentTimeMillis();

        long embedded = uuid.getMostSignificantBits() >>> 16;

        assertThat(embedded).isBetween(before, after);
    }

    @Test
    void identifierFactoriesProduceDistinctValues() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            generated.add(IdGenerator.correlationId());
            generated.add(IdGenerator.eventId());
            generated.add(IdGenerator.idempotencyKey());
            generated.add(IdGenerator.newId());
            generated.add(IdGenerator.newUuidV7());
        }

        assertThat(generated).hasSize(5_000);
    }

    @Test
    void journeyIdentifiersAreTimeSortable() {
        assertThat(Ulid.isValid(IdGenerator.correlationId())).isTrue();
        assertThat(Ulid.isValid(IdGenerator.eventId())).isTrue();
        assertThat(Ulid.isValid(IdGenerator.idempotencyKey())).isTrue();
    }
}
