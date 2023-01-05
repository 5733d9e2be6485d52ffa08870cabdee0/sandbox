package com.redhat.service.smartevents.infra.v2.api.models;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ManagedResourceStatusV2Test {

    @ParameterizedTest
    @EnumSource(ManagedResourceStatusV2.class)
    void testSerialisation(ManagedResourceStatusV2 status) {
        assertThat(status.getValue()).isEqualTo(status.name().toLowerCase(Locale.ROOT));
    }

    @ParameterizedTest
    @EnumSource(ManagedResourceStatusV2.class)
    void testFromString(ManagedResourceStatusV2 status) {
        assertThat(ManagedResourceStatusV2.fromString(status.value)).isEqualTo(status);
    }

    @Test
    void testFromStringWithInvalidValue() {
        assertThatThrownBy(() -> ManagedResourceStatusV2.fromString("bananna")).isInstanceOf(IllegalArgumentException.class);
    }

}
