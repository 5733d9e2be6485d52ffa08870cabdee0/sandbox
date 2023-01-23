package com.redhat.service.smartevents.manager.v2.providers;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.core.providers.ResourcePrefixProviderImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceNamesProviderV2Test {

    private static final String RFC1035_REGEX = "^[a-z]([a-z0-9-]{0,61}[a-z0-9])?$";
    private static final String TEST_BRIDGE_ID = "0eb9c28e-ede3-49f3-a955-465a9e8d1487";

    @Test
    void testValidPrefix() {
        String prefix = "ob-dev";
        String validatedPrefix = "ob-dev-";
        ResourcePrefixProviderImpl resourcePrefixProvider = new ResourcePrefixProviderImpl(prefix);
        assertThat(resourcePrefixProvider.getValidatedResourcePrefix()).isEqualTo(validatedPrefix);
        ResourceNamesProviderV2 resourceNamesProviderV1 = new ResourceNamesProviderV2Impl(resourcePrefixProvider);

        String processorConnectorName = resourceNamesProviderV1.getSourceConnectorName(TEST_BRIDGE_ID);
        assertIsRFC1035Label(processorConnectorName);
        assertThat(processorConnectorName).isEqualTo(validatedPrefix + ResourceNamesProviderV2Impl.SOURCE_CONNECTOR_SHORTNAME + "-" + TEST_BRIDGE_ID);

        String processorTopicName = resourceNamesProviderV1.getSourceConnectorTopicName(TEST_BRIDGE_ID);
        assertIsRFC1035Label(processorTopicName);
        assertThat(processorTopicName).isEqualTo(validatedPrefix + ResourceNamesProviderV2Impl.SOURCE_CONNECTOR_SHORTNAME + "-" + TEST_BRIDGE_ID);
    }

    private static void assertIsRFC1035Label(String label) {
        // assert that the resource name conforms to the most strict resource naming supported by Kubernetes
        // check: https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#rfc-1035-label-names
        assertThat(label).matches(RFC1035_REGEX);
    }
}
