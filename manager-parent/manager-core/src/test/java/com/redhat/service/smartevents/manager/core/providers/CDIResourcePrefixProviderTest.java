package com.redhat.service.smartevents.manager.core.providers;

import javax.inject.Inject;

import org.junit.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CDIResourcePrefixProviderTest {

    @Inject
    ResourcePrefixProvider resourcePrefixProvider;

    @Test
    public void testGetValidatedPrefix() {
        assertThat(resourcePrefixProvider.getValidatedResourcePrefix()).isEqualTo("ob-tests-");
    }
}
