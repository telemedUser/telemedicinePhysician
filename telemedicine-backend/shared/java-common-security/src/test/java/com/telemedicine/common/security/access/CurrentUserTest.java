package com.telemedicine.common.security.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.telemedicine.common.security.Roles;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CurrentUserTest {

    @Test
    void anonymousIsAUsableValueRatherThanNull() {
        CurrentUser anonymous = CurrentUser.anonymous();

        assertThat(anonymous.authenticated()).isFalse();
        assertThat(anonymous.roles()).isEmpty();
        assertThat(anonymous.scopes()).isEmpty();
        assertThat(anonymous.hasRole(Roles.ADMIN)).isFalse();
    }

    @Test
    @DisplayName("an audit row always gets an actor, even for an unauthenticated call")
    void actorIdIsNeverNull() {
        assertThat(CurrentUser.anonymous().actorId()).isEqualTo("anonymous");
        assertThat(new CurrentUser("2b3c", null, null, null, Set.of(), Set.of(), true).actorId())
                .isEqualTo("2b3c");
    }

    @Test
    void nullCollectionsAreNormalisedAndTheSnapshotIsImmutable() {
        CurrentUser user = new CurrentUser("id", null, null, null, null, null, true);

        assertThat(user.roles()).isEmpty();
        assertThat(user.scopes()).isEmpty();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> user.roles().add(Roles.ADMIN));
    }

    @Test
    void copiesTheCollectionsItIsGiven() {
        Set<String> roles = new HashSet<>(Set.of(Roles.PATIENT));
        CurrentUser user = new CurrentUser("id", null, null, null, roles, Set.of(), true);
        roles.add(Roles.ADMIN);

        assertThat(user.roles()).containsExactly(Roles.PATIENT);
    }
}
