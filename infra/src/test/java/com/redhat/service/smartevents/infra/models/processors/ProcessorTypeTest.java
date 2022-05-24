package com.redhat.service.smartevents.infra.models.processors;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProcessorTypeTest {

    @ParameterizedTest
    @EnumSource(ProcessorType.class)
    void testSerialisation(ProcessorType type) {
        assertThat(type.getValue()).isEqualTo(type.name().toLowerCase(Locale.ROOT));
    }

    @ParameterizedTest
    @EnumSource(ProcessorType.class)
    void testFromString(ProcessorType type) {
        assertThat(ProcessorType.fromString(type.value)).isEqualTo(type);
    }

    @Test
    void testFromStringWithInvalidValue() {
        assertThatThrownBy(() -> ProcessorType.fromString("bananna")).isInstanceOf(IllegalArgumentException.class);
    }

}
