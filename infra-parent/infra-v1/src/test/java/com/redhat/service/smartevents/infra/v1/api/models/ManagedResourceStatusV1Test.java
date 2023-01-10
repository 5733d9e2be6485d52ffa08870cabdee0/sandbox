package com.redhat.service.smartevents.infra.v1.api.models;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ManagedResourceStatusV1Test {

    @ParameterizedTest
    @EnumSource(ManagedResourceStatusV1.class)
    void testSerialisation(ManagedResourceStatusV1 status) {
        assertThat(status.getValue()).isEqualTo(status.name().toLowerCase(Locale.ROOT));
    }

    @ParameterizedTest
    @EnumSource(ManagedResourceStatusV1.class)
    void testFromString(ManagedResourceStatusV1 status) {
        assertThat(ManagedResourceStatusV1.fromString(status.value)).isEqualTo(status);
    }

    @Test
    void testFromStringWithInvalidValue() {
        assertThatThrownBy(() -> ManagedResourceStatusV1.fromString("bananna")).isInstanceOf(IllegalArgumentException.class);
    }

}
