package com.redhat.service.smartevents.manager.v1.providers;

import com.redhat.service.smartevents.manager.core.providers.ResourcePrefixProviderImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceNamesProviderV1Test {

    private static final String RFC1035_REGEX = "^[a-z]([a-z0-9-]{0,61}[a-z0-9])?$";
    private static final String TEST_PROCESSOR_ID = "53a00276-7e41-42e2-8606-50faece6a7ab";

    void testValidPrefix() {
        String prefix = "ob-dev";
        String validatedPrefix = "ob-dev-";
        ResourcePrefixProviderImpl resourcePrefixProvider = new ResourcePrefixProviderImpl(prefix);
        assertThat(resourcePrefixProvider.getValidatedResourcePrefix()).isEqualTo(validatedPrefix);
        ResourceNamesProviderV1 resourceNamesProviderV1 = new ResourceNamesProviderV1Impl(resourcePrefixProvider);

        String processorConnectorName = resourceNamesProviderV1.getProcessorConnectorName(TEST_PROCESSOR_ID);
        assertIsRFC1035Label(processorConnectorName);
        assertThat(processorConnectorName).isEqualTo(validatedPrefix + ResourceNamesProviderV1Impl.PROCESSOR_SHORTNAME + "-" + TEST_PROCESSOR_ID);

        String processorTopicName = resourceNamesProviderV1.getProcessorTopicName(TEST_PROCESSOR_ID);
        assertIsRFC1035Label(processorTopicName);
        assertThat(processorTopicName).isEqualTo(validatedPrefix + ResourceNamesProviderV1Impl.PROCESSOR_SHORTNAME + "-" + TEST_PROCESSOR_ID);
    }

    private static void assertIsRFC1035Label(String label) {
        // assert that the resource name conforms to the most strict resource naming supported by Kubernetes
        // check: https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#rfc-1035-label-names
        assertThat(label).matches(RFC1035_REGEX);
    }
}
