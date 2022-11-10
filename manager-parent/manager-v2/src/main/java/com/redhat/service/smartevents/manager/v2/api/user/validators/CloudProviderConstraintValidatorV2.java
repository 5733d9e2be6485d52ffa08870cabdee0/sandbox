package com.redhat.service.smartevents.manager.v2.api.user.validators;

import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.InvalidCloudProviderException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.InvalidRegionException;
import com.redhat.service.smartevents.manager.core.api.validators.BaseConstraintValidator;
import com.redhat.service.smartevents.manager.core.persistence.dao.CloudProviderDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudProvider;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudRegion;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequestV2;

@ApplicationScoped
public class CloudProviderConstraintValidatorV2 extends BaseConstraintValidator<ValidCloudProviderV2, BridgeRequestV2> {

    static final String ID_PARAM = "id";

    static final String REGION_PARAM = "region";

    static final String CLOUD_PROVIDER_NOT_VALID = "The requested Cloud Provider '{id}' is not valid.";

    static final String CLOUD_PROVIDER_NOT_ENABLED = "The requested Cloud Provider '{id}' is not enabled.";

    static final String CLOUD_REGION_NOT_VALID = "The requested Region '{region}' is not valid.";

    static final String CLOUD_REGION_NOT_ENABLED = "The requested Region '{region}' is not enabled.";

    @Inject
    CloudProviderDAO cloudProviderDAO;

    @Override
    public boolean isValid(BridgeRequestV2 bridgeRequestV2, ConstraintValidatorContext context) {

        boolean valid = true;

        if (StringUtils.isEmpty(bridgeRequestV2.getCloudProvider()) || StringUtils.isEmpty(bridgeRequestV2.getRegion())) {
            valid = false;
        }

        CloudProvider cloudProvider = null;

        if (!StringUtils.isEmpty(bridgeRequestV2.getCloudProvider())) {
            cloudProvider = cloudProviderDAO.findById(bridgeRequestV2.getCloudProvider());
            if (cloudProvider == null) {
                addConstraintViolation(context, CLOUD_PROVIDER_NOT_VALID, Collections.singletonMap(ID_PARAM, bridgeRequestV2.getCloudProvider()), InvalidCloudProviderException::new);
                valid = false;
            } else {
                if (!cloudProvider.isEnabled()) {
                    addConstraintViolation(context, CLOUD_PROVIDER_NOT_ENABLED, Collections.singletonMap(ID_PARAM, bridgeRequestV2.getCloudProvider()), InvalidCloudProviderException::new);
                    valid = false;
                }
            }
        }

        Optional<CloudRegion> region = Optional.empty();

        if (cloudProvider != null) {
            region = cloudProvider.getRegionByName(bridgeRequestV2.getRegion());
        }

        if (region.isEmpty() && !StringUtils.isEmpty(bridgeRequestV2.getRegion())) {
            addConstraintViolation(context, CLOUD_REGION_NOT_VALID, Collections.singletonMap(REGION_PARAM, bridgeRequestV2.getRegion()), InvalidRegionException::new);
            valid = false;
        } else {
            if (!region.isEmpty()) {
                if (!region.get().isEnabled()) {
                    addConstraintViolation(context, CLOUD_REGION_NOT_ENABLED, Collections.singletonMap(REGION_PARAM, bridgeRequestV2.getRegion()), InvalidRegionException::new);
                    valid = false;
                }
            }
        }

        return valid;
    }
}
