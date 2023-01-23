package com.redhat.service.smartevents.manager.core.providers;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ResourcePrefixProviderTest {

    private static final String[][] VALID_PREFIXES = {
            { "ob-dev", "ob-dev-" },
            { "ob-stable-", "ob-stable-" },
            { "number1234", "number1234-" },
            { "this-is-valid-", "this-is-valid-" },
            { "bla---xyz", "bla---xyz-" },
            { "abc12345678987654", "abc12345678987654-" },
            { "ci-2753611352-1", "ci-2753611352-1-" },
            { "ci-2753611352-999", "ci-2753611352-999-" }
    };

    private static final String[] INVALID_PREFIXES = {
            "1",
            "_",
            "test_",
            "SoMeTh1nG",
            "dollar$",
            "",
            "abcdeabcdeabcdeabcdea",
            "-notvalid"
    };

    @ParameterizedTest
    @MethodSource("validPrefixes")
    void testValidPrefix(String prefix, String expectedValidatedPrefix) {
        ResourcePrefixProviderImpl resourcePrefixProvider = new ResourcePrefixProviderImpl(prefix);
        assertThat(resourcePrefixProvider.getValidatedResourcePrefix()).isEqualTo(expectedValidatedPrefix);
    }

    private static Stream<Arguments> validPrefixes() {
        return Arrays.stream(VALID_PREFIXES).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("invalidPrefixes")
    void testInvalidPrefix(String prefix) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ResourcePrefixProviderImpl(prefix));
    }

    private static Stream<Arguments> invalidPrefixes() {
        return Arrays.stream(INVALID_PREFIXES).map(Arguments::of);
    }
}
