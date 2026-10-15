package com.telemedicine.simulators.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AppointmentStateStoreTest {

    private AppointmentStateStore store;

    @BeforeEach
    void setUp() {
        store = new AppointmentStateStore();
    }

    @Test
    void createsScheduledAppointment() {
        AppointmentState state = store.getOrCreate("apt-001");
        assertThat(state.status()).isEqualTo("SCHEDULED");
        assertThat(state.paymentStatus()).isEqualTo("UNPAID");
    }

    @Test
    void allowsValidTransition() {
        store.getOrCreate("apt-001");
        AppointmentState updated = store.transition("apt-001", "CHECKED_IN");
        assertThat(updated.status()).isEqualTo("CHECKED_IN");
    }

    @Test
    void rejectsInvalidTransition() {
        store.getOrCreate("apt-001");
        store.transition("apt-001", "CHECKED_IN");
        assertThatThrownBy(() -> store.transition("apt-001", "SCHEDULED"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void marksPaid() {
        store.markPaid("apt-001");
        assertThat(store.getOrCreate("apt-001").paymentStatus()).isEqualTo("PAID");
    }
}
