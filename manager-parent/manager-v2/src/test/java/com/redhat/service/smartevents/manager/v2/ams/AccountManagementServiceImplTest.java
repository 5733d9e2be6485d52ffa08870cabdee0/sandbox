package com.redhat.service.smartevents.manager.v2.ams;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.manager.v2.TestConstants;

import io.quarkus.test.junit.QuarkusTest;

import dev.bf2.ffm.ams.core.models.AccountInfo;
import dev.bf2.ffm.ams.core.models.CreateResourceRequest;
import dev.bf2.ffm.ams.core.models.TermsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class AccountManagementServiceImplTest {

    @Inject
    AccountManagementServiceImpl accountManagementService;

    @Test
    public void testOrganizationWithNoQuota() {
        CreateResourceRequest createResourceRequest = new CreateResourceRequest.Builder()
                .withCount(1)
                .withProductId("TODO")
                .withCloudProviderId("TODO")
                .withAvailabilityZoneType("TODO")
                .withResourceName("TODO")
                .withBillingModel("TODO")
                .withClusterId("TODO")
                .withTermRequest(new TermsRequest.Builder().withEventCode("TODO").withSiteCode("TODO").build())
                .withAccountInfo(new AccountInfo.Builder()
                        .withOrganizationId("no_an_option!")
                        .withAccountUsername("TODO")
                        .withAccountId(0L)
                        .withAdminRole(Boolean.FALSE)
                        .build())
                .build();

        assertThatThrownBy(() -> accountManagementService.createResource(createResourceRequest).await().atMost(Duration.ofSeconds(5))).isInstanceOf(NoQuotaAvailable.class);
    }

    @Test
    public void testCreateResource() {
        CreateResourceRequest createResourceRequest = new CreateResourceRequest.Builder()
                .withCount(1)
                .withProductId("TODO")
                .withCloudProviderId("TODO")
                .withAvailabilityZoneType("TODO")
                .withResourceName("TODO")
                .withBillingModel("TODO")
                .withClusterId("TODO")
                .withTermRequest(new TermsRequest.Builder().withEventCode("TODO").withSiteCode("TODO").build())
                .withAccountInfo(new AccountInfo.Builder()
                        .withOrganizationId(TestConstants.DEFAULT_ORGANISATION_ID)
                        .withAccountUsername("TODO")
                        .withAccountId(0L)
                        .withAdminRole(Boolean.FALSE)
                        .build())
                .build();

        String subscriptionId = accountManagementService.createResource(createResourceRequest).await().atMost(Duration.ofSeconds(5)).getSubscriptionId();
        assertThat(subscriptionId).isNotBlank().isNotNull();
    }

    @Test
    public void testOrganisationHasAvailableQuota() {
        AccountInfo accountInfo = new AccountInfo.Builder()
                .withOrganizationId(TestConstants.DEFAULT_ORGANISATION_ID)
                .withAccountUsername("TODO")
                .withAccountId(0L)
                .withAdminRole(Boolean.FALSE)
                .build();

        assertThat(accountManagementService.organizationHasAvailableQuota(accountInfo, "TODO", "TODO").await().atMost(Duration.ofSeconds(5))).isTrue();
    }

    @Test
    public void testOrganisationHasNoAvailableQuota() {
        AccountInfo accountInfo = new AccountInfo.Builder()
                .withOrganizationId("not an org")
                .withAccountUsername("TODO")
                .withAccountId(0L)
                .withAdminRole(Boolean.FALSE)
                .build();

        assertThat(accountManagementService.organizationHasAvailableQuota(accountInfo, "TODO", "TODO").await().atMost(Duration.ofSeconds(5))).isFalse();
    }
}
