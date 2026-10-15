package com.telemedicine.common.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class TopicNamesTest {

    @Test
    void resolvesMainRetryAndDlqTopics() {
        assertThat(TopicNames.topicFor(EventTypes.LINK_GENERATED)).isEqualTo("LinkGenerated");
        assertThat(TopicNames.dlqFor(EventTypes.LINK_GENERATED)).isEqualTo("LinkGenerated.DLQ");
        assertThat(TopicNames.retryFor(EventTypes.LINK_GENERATED)).isEqualTo("LinkGenerated.RETRY");
    }

    @Test
    void allTopicsHasThreeEntriesPerEvent() {
        assertThat(TopicNames.allTopics()).hasSize(EventTypes.ALL.size() * 3);
    }

    @Test
    void dlqForTopicStripsRetrySuffix() {
        assertThat(TopicNames.dlqForTopic("LinkGenerated.RETRY")).isEqualTo("LinkGenerated.DLQ");
    }

    @Test
    void dlqForTopicRejectsNullTopic() {
        assertThatIllegalArgumentException().isThrownBy(() -> TopicNames.dlqForTopic(null));
    }

    @Test
    void dlqForTopicHandlesEdgeCases() {
        assertThat(TopicNames.dlqForTopic("LinkGenerated.DLQ")).isEqualTo("LinkGenerated.DLQ");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TopicNames.dlqForTopic("  "))
                .withMessageContaining("Topic is required");
    }

    @Test
    void requireRejectsUnknownTypes() {
        assertThatIllegalArgumentException().isThrownBy(() -> TopicNames.topicFor("NotAnEvent"));
    }
}
