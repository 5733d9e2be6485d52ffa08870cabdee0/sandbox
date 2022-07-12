package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

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

    static final String CLOUD_PROVIDER_REQUIRED = "Cloud Provider cannot be null or empty.";

    static final String CLOUD_REGION_REQUIRED = "Cloud Region cannot be null or empty.";

    static final String CLOUD_PROVIDER_NOT_VALID = "The requested Cloud Provider '{id}' is not valid.";

    static final String CLOUD_PROVIDER_NOT_ENABLED = "The requested Cloud Provider '{id}' is not enabled.";

    static final String CLOUD_REGION_NOT_VALID = "The requested Region '{region}' is not valid.";

    static final String CLOUD_REGION_NOT_ENABLED = "The requested Region '{region}' is not enabled.";

    @Inject
    CloudProviderDAO cloudProviderDAO;

    @Override
    public boolean isValid(BridgeRequest bridgeRequest, ConstraintValidatorContext context) {

        boolean valid = true;

        if (StringUtils.isEmpty(bridgeRequest.getCloudProvider())) {
            addConstraintViolation(context, CLOUD_PROVIDER_REQUIRED, new HashMap<>(), InvalidCloudProviderException::new);
            valid = false;
        }

        if (StringUtils.isEmpty(bridgeRequest.getRegion())) {
            addConstraintViolation(context, CLOUD_REGION_REQUIRED, new HashMap<>(), InvalidRegionException::new);
            valid = false;
        }

        CloudProvider cloudProvider = null;

        if (!StringUtils.isEmpty(bridgeRequest.getCloudProvider())) {
            cloudProvider = cloudProviderDAO.findById(bridgeRequest.getCloudProvider());
            if (cloudProvider == null) {
                addConstraintViolation(context, CLOUD_PROVIDER_NOT_VALID, Collections.singletonMap(ID_PARAM, bridgeRequest.getCloudProvider()), InvalidCloudProviderException::new);
                valid = false;
            } else {
                if (!cloudProvider.isEnabled()) {
                    addConstraintViolation(context, CLOUD_PROVIDER_NOT_ENABLED, Collections.singletonMap(ID_PARAM, bridgeRequest.getCloudProvider()), InvalidCloudProviderException::new);
                    valid = false;
                }
            }
        }

        Optional<CloudRegion> region = Optional.empty();

        if (cloudProvider != null) {
            region = cloudProvider.getRegionByName(bridgeRequest.getRegion());
        }

        if (region.isEmpty() && !StringUtils.isEmpty(bridgeRequest.getRegion())) {
            addConstraintViolation(context, CLOUD_REGION_NOT_VALID, Collections.singletonMap(REGION_PARAM, bridgeRequest.getRegion()), InvalidRegionException::new);
            valid = false;
        } else {
            if (!region.isEmpty()) {
                if (!region.get().isEnabled()) {
                    addConstraintViolation(context, CLOUD_REGION_NOT_ENABLED, Collections.singletonMap(REGION_PARAM, bridgeRequest.getRegion()), InvalidRegionException::new);
                    valid = false;
                }
            }
        }

        return valid;
    }
}
