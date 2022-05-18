package com.redhat.service.smartevents.executor;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class CloudEventExtensionTest {

    @ParameterizedTest
    @MethodSource("extensionToBeConverted")
    void testConversion(String input, String expected) {
        assertThat(CloudEventExtension.adjustExtensionName(input)).isEqualTo(expected);
    }

    @Test
    void nonValidNullInput() {
        assertThatThrownBy(() -> CloudEventExtension.adjustExtensionName(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> extensionToBeConverted() {
        Object[][] arguments = {
                { "test connector 1", "testconnector1" },
                { "", "" },
                { "test-connector", "testconnector" },
                { "____extension`@!#^%&*", "extension" },
        };
        return Stream.of(arguments).map(Arguments::of);
    }
}