package com.redhat.service.smartevents.manager.providers;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ResourceNamesProviderTest {

    private static final String RFC1035_REGEX = "^[a-z]([a-z0-9-]{0,61}[a-z0-9])?$";
    private static final String TEST_BRIDGE_ID = "70afa003-f7c7-4151-a1aa-1530572c37d0";
    private static final String TEST_PROCESSOR_ID = "53a00276-7e41-42e2-8606-50faece6a7ab";
    private static final String ACTION_NAME = "actionName";

    private static final String[][] VALID_PREFIXES = {
            { "ob-dev", "ob-dev-" },
            { "ob-stable-", "ob-stable-" },
            { "number1234", "number1234-" },
            { "this-is-valid-", "this-is-valid-" },
            { "bla---xyz", "bla---xyz-" },
            { "abc12345678987654321", "abc12345678987654321-" }
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

//    @ParameterizedTest
//    @MethodSource("validPrefixes")
//    void testValidPrefix(String prefix, String expectedValidatedPrefix) {
//        ResourceNamesProviderImpl resourceNamesProvider = new ResourceNamesProviderImpl();
//        resourceNamesProvider.resourcePrefix = prefix;
//        resourceNamesProvider.validate();
//        assertThat(resourceNamesProvider.validatedResourcePrefix).isEqualTo(expectedValidatedPrefix);
//
//        String bridgeTopicName = resourceNamesProvider.getBridgeTopicName(TEST_BRIDGE_ID);
//        assertIsRFC1035Label(bridgeTopicName);
//        assertThat(bridgeTopicName).isEqualTo(expectedValidatedPrefix + ResourceNamesProviderImpl.BRIDGE_SHORTNAME + "-" + TEST_BRIDGE_ID);
//
//        String processorConnectorName = resourceNamesProvider.getProcessorConnectorName(TEST_PROCESSOR_ID, ACTION_NAME);
//        assertIsRFC1035Label(processorConnectorName);
//        assertThat(processorConnectorName).isEqualTo(expectedValidatedPrefix + ResourceNamesProviderImpl.PROCESSOR_SHORTNAME + "-" + TEST_PROCESSOR_ID);
//
//        String processorTopicName = resourceNamesProvider.getProcessorTopicName(TEST_PROCESSOR_ID, ACTION_NAME);
//        assertIsRFC1035Label(processorTopicName);
//        assertThat(processorTopicName).isEqualTo(expectedValidatedPrefix + ResourceNamesProviderImpl.PROCESSOR_SHORTNAME + "-" + TEST_PROCESSOR_ID);
//    }

    private static Stream<Arguments> validPrefixes() {
        return Arrays.stream(VALID_PREFIXES).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("invalidPrefixes")
    void testInvalidPrefix(String prefix) {
        ResourceNamesProviderImpl resourceNamesProvider = new ResourceNamesProviderImpl();
        resourceNamesProvider.resourcePrefix = prefix;
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(resourceNamesProvider::validate);
    }

    private static Stream<Arguments> invalidPrefixes() {
        return Arrays.stream(INVALID_PREFIXES).map(Arguments::of);
    }

    private static void assertIsRFC1035Label(String label) {
        // assert that the resource name conforms to the most strict resource naming supported by Kubernetes
        // check: https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#rfc-1035-label-names
        assertThat(label).matches(RFC1035_REGEX);
    }

}
