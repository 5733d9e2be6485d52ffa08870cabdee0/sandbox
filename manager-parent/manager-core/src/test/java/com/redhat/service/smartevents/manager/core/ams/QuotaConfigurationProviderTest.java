package com.redhat.service.smartevents.manager.core.ams;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.core.TestConstants;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class QuotaConfigurationProviderTest {

    @Inject
    QuotaConfigurationProvider quotaConfigurationProvider;

    @Test
    public void testConfigurationForTests() {
        assertThat(quotaConfigurationProvider.getOrganisationQuotas(TestConstants.DEFAULT_ORGANISATION_ID).getProcessorsQuota()).isEqualTo(100);
        assertThat(quotaConfigurationProvider.getOrganisationQuotas(TestConstants.DEFAULT_ORGANISATION_ID).getBridgesQuota()).isEqualTo(100);
    }

    @Test
    public void testOrganizationWithNoQuota() {
        assertThat(quotaConfigurationProvider.getOrganisationQuotas("").getProcessorsQuota()).isEqualTo(0);
        assertThat(quotaConfigurationProvider.getOrganisationQuotas("").getBridgesQuota()).isEqualTo(0);
    }
}
