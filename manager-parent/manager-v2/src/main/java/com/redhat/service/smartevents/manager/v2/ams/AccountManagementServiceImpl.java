package com.redhat.service.smartevents.manager.v2.ams;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;

import io.smallrye.mutiny.Uni;

import dev.bf2.ffm.ams.core.AccountManagementService;
import dev.bf2.ffm.ams.core.models.AccountInfo;
import dev.bf2.ffm.ams.core.models.CreateResourceRequest;
import dev.bf2.ffm.ams.core.models.ResourceCreated;

// TODO: Remove this class and replace with https://github.com/bf2fc6cc711aee1a0c2a/ffm-fleet-manager-java-sdk/blob/b0a109f5f4704abc14aa44cdd6ee2c20425e649a/ams/ams-core/src/main/java/dev/bf2/ffm/ams/core/AccountManagementService.java#L13 when https://issues.redhat.com/browse/MGDOBR-1166 is started
// TODO {manstis} This probably needs all Bridges across all versions?
@ApplicationScoped
public class AccountManagementServiceImpl implements AccountManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountManagementServiceImpl.class);

    @Inject
    QuotaConfigurationProvider quotaConfigurationProvider;

    @Inject
    BridgeDAO bridgeDAO;

    @Override
    public Uni<ResourceCreated> createResource(CreateResourceRequest createResourceRequest) {
        if (organisationHasBridgesQuota(createResourceRequest.getAccountInfo().getOrganizationId())) {
            // TODO: Change with specific exception
            return Uni.createFrom().failure(
                    new NoQuotaAvailable(
                            String.format("The organisation '%s' has already reached the quota limit for v2 instances.", createResourceRequest.getAccountInfo().getOrganizationId())));
        }

        return Uni.createFrom().item(new ResourceCreated.Builder().withSubscriptionId(UUID.randomUUID().toString()).build());
    }

    @Override
    public Uni<Void> deleteResource(String resourceId) {
        // Do Nothing
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Boolean> organizationHasAvailableQuota(AccountInfo accountInfo, String productId, String resourceName) {
        if (organisationHasBridgesQuota(accountInfo.getOrganizationId())) {
            // TODO: Change with specific exception
            return Uni.createFrom().item(Boolean.FALSE);
        }
        return Uni.createFrom().item(Boolean.TRUE);
    }

    private boolean organisationHasBridgesQuota(String organisationId) {
        long organisationQuota = quotaConfigurationProvider.getOrganisationQuotas(organisationId).getBridgesQuota();
        long organisationConsumption = bridgeDAO.countByOrganisationId(organisationId);
        LOGGER.debug("Organization id '{}' has '{}' bridge instances where the limit is '{}' for v2 instances", organisationId, organisationConsumption, organisationQuota);
        return organisationConsumption + 1 > organisationQuota;
    }
}
