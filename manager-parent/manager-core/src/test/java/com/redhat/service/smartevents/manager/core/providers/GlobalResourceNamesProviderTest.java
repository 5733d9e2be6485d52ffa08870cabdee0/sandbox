package com.redhat.service.smartevents.manager.core.providers;

import org.junit.jupiter.api.Test;

import static com.redhat.service.smartevents.manager.core.providers.GlobalResourceNamesProviderImpl.ERROR_TOPIC_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalResourceNamesProviderTest {

    private static final String RFC1035_REGEX = "^[a-z]([a-z0-9-]{0,61}[a-z0-9])?$";
    private static final String TEST_BRIDGE_ID = "70afa003-f7c7-4151-a1aa-1530572c37d0";

    @Test
    void testValidPrefix() {
        String prefix = "ob-dev";
        String validatedPrefix = "ob-dev-";
        ResourcePrefixProviderImpl resourcePrefixProvider = new ResourcePrefixProviderImpl(prefix);
        assertThat(resourcePrefixProvider.getValidatedResourcePrefix()).isEqualTo(validatedPrefix);
        GlobalResourceNamesProviderImpl resourceNamesProvider = new GlobalResourceNamesProviderImpl(resourcePrefixProvider);

        String bridgeTopicName = resourceNamesProvider.getBridgeTopicName(TEST_BRIDGE_ID);
        assertIsRFC1035Label(bridgeTopicName);
        assertThat(bridgeTopicName).isEqualTo(validatedPrefix + GlobalResourceNamesProviderImpl.BRIDGE_SHORTNAME + "-" + TEST_BRIDGE_ID);

        String bridgeErrorTopicName = resourceNamesProvider.getBridgeErrorTopicName(TEST_BRIDGE_ID);
        assertIsRFC1035Label(bridgeErrorTopicName);
        assertThat(bridgeErrorTopicName).isEqualTo(validatedPrefix + GlobalResourceNamesProviderImpl.BRIDGE_SHORTNAME + "-" + TEST_BRIDGE_ID + "-" + ERROR_TOPIC_SUFFIX);

        String globalErrorTopicName = resourceNamesProvider.getGlobalErrorTopicName();
        assertIsRFC1035Label(globalErrorTopicName);
        assertThat(globalErrorTopicName).isEqualTo(validatedPrefix + ERROR_TOPIC_SUFFIX);
    }

    private static void assertIsRFC1035Label(String label) {
        // assert that the resource name conforms to the most strict resource naming supported by Kubernetes
        // check: https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#rfc-1035-label-names
        assertThat(label).matches(RFC1035_REGEX);
    }

}
