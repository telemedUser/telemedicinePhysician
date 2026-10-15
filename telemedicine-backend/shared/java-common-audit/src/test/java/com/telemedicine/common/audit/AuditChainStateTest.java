package com.telemedicine.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditChainStateTest {

    @Test
    void advancesSequenceAndPreviousHash() {
        AuditChainState state = new AuditChainState();
        AuditChainState.ChainLink first = state.nextLink();
        assertThat(first.sequence()).isEqualTo(1L);
        assertThat(first.previousHash()).isEmpty();

        state.advance("hash-1");
        AuditChainState.ChainLink second = state.nextLink();
        assertThat(second.sequence()).isEqualTo(2L);
        assertThat(second.previousHash()).isEqualTo("hash-1");
        assertThat(state.lastSequence()).isEqualTo(2L);
        assertThat(state.lastHash()).isEqualTo("hash-1");
    }
}
