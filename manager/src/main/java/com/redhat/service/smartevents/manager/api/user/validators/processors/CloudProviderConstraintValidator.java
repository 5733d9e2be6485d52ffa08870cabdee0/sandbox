package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.InvalidCloudProviderException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.InvalidRegionException;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.dao.CloudProviderDAO;
import com.redhat.service.smartevents.manager.models.CloudProvider;
import com.redhat.service.smartevents.manager.models.CloudRegion;

@ApplicationScoped
public class CloudProviderConstraintValidator extends BaseConstraintValidator<ValidCloudProvider, BridgeRequest> {

    static final String ID_PARAM = "id";

    static final String REGION_PARAM = "region";

    static final String CLOUD_PROVIDER_NOT_VALID = "Cloud Provider '{id}' is not valid.";

    static final String CLOUD_PROVIDER_NOT_ENABLED = "Cloud Provider '{id}' is not enabled.";

    static final String CLOUD_REGION_NOT_VALID = "The Region '{region}' for Cloud Provider '{id}' is not valid.";

    static final String CLOUD_REGION_NOT_ENABLED = "The Region '{region}' for Cloud Provider '{id}' is not enabled.";

    @Inject
    CloudProviderDAO cloudProviderDAO;

    @Override
    public boolean isValid(BridgeRequest bridgeRequest, ConstraintValidatorContext context) {
        CloudProvider cloudProvider = cloudProviderDAO.findById(bridgeRequest.getCloudProvider());
        if (cloudProvider == null) {
            addConstraintViolation(context, CLOUD_PROVIDER_NOT_VALID, Collections.singletonMap(ID_PARAM, bridgeRequest.getCloudProvider()), InvalidCloudProviderException::new);
            return false;
        }

        if (!cloudProvider.isEnabled()) {
            addConstraintViolation(context, CLOUD_PROVIDER_NOT_ENABLED, Collections.singletonMap(ID_PARAM, bridgeRequest.getCloudProvider()), InvalidCloudProviderException::new);
            return false;
        }

        Optional<CloudRegion> region = cloudProvider.getRegionByName(bridgeRequest.getRegion());
        if (region.isEmpty()) {
            addConstraintViolation(context, CLOUD_REGION_NOT_VALID, createRegionParams(cloudProvider, bridgeRequest.getRegion()), InvalidRegionException::new);
            return false;
        }

        if (!region.get().isEnabled()) {
            addConstraintViolation(context, CLOUD_REGION_NOT_ENABLED, createRegionParams(cloudProvider, bridgeRequest.getRegion()), InvalidRegionException::new);
            return false;
        }

        return true;
    }

    private Map<String, Object> createRegionParams(CloudProvider cloudProvider, String requestedRegion) {
        Map<String, Object> params = new HashMap<>();
        params.put(ID_PARAM, cloudProvider.getId());
        params.put(REGION_PARAM, requestedRegion);
        return params;
    }
}
