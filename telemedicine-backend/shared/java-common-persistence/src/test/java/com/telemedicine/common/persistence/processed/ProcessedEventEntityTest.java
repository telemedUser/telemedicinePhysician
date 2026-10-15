package com.telemedicine.common.persistence.processed;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.persistence.processed.ProcessedEventEntity.ProcessedEventId;
import org.junit.jupiter.api.Test;

class ProcessedEventEntityTest {

    @Test
    void compositeKeyEqualsAndHashes() {
        ProcessedEventId left = new ProcessedEventId("group", "event");
        ProcessedEventId right = new ProcessedEventId("group", "event");
        ProcessedEventId different = new ProcessedEventId("group", "other");

        assertThat(left).isEqualTo(right).hasSameHashCodeAs(right);
        assertThat(left).isNotEqualTo(different);
    }
}
