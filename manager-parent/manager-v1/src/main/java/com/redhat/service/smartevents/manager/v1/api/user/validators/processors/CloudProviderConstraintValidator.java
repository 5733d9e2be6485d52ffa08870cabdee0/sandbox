package com.redhat.service.smartevents.manager.v1.api.user.validators.processors;

import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.InvalidCloudProviderException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.InvalidRegionException;
import com.redhat.service.smartevents.manager.core.api.validators.BaseConstraintValidator;
import com.redhat.service.smartevents.manager.core.persistence.dao.CloudProviderDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudProvider;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudRegion;
import com.redhat.service.smartevents.manager.v1.api.models.requests.BridgeRequestV1;
import org.apache.commons.lang3.StringUtils;

@ApplicationScoped
public class CloudProviderConstraintValidator extends BaseConstraintValidator<ValidCloudProvider, BridgeRequestV1> {

    static final String ID_PARAM = "id";

    static final String REGION_PARAM = "region";

    static final String CLOUD_PROVIDER_NOT_VALID = "The requested Cloud Provider '{id}' is not valid.";

    static final String CLOUD_PROVIDER_NOT_ENABLED = "The requested Cloud Provider '{id}' is not enabled.";

    static final String CLOUD_REGION_NOT_VALID = "The requested Region '{region}' is not valid.";

    static final String CLOUD_REGION_NOT_ENABLED = "The requested Region '{region}' is not enabled.";

    @Inject
    CloudProviderDAO cloudProviderDAO;

    @Override
    public boolean isValid(BridgeRequestV1 bridgeRequestV1, ConstraintValidatorContext context) {

        boolean valid = true;

        if (StringUtils.isEmpty(bridgeRequestV1.getCloudProvider()) || StringUtils.isEmpty(bridgeRequestV1.getRegion())) {
            valid = false;
        }

        CloudProvider cloudProvider = null;

        if (!StringUtils.isEmpty(bridgeRequestV1.getCloudProvider())) {
            cloudProvider = cloudProviderDAO.findById(bridgeRequestV1.getCloudProvider());
            if (cloudProvider == null) {
                addConstraintViolation(context, CLOUD_PROVIDER_NOT_VALID, Collections.singletonMap(ID_PARAM, bridgeRequestV1.getCloudProvider()), InvalidCloudProviderException::new);
                valid = false;
            } else {
                if (!cloudProvider.isEnabled()) {
                    addConstraintViolation(context, CLOUD_PROVIDER_NOT_ENABLED, Collections.singletonMap(ID_PARAM, bridgeRequestV1.getCloudProvider()), InvalidCloudProviderException::new);
                    valid = false;
                }
            }
        }

        Optional<CloudRegion> region = Optional.empty();

        if (cloudProvider != null) {
            region = cloudProvider.getRegionByName(bridgeRequestV1.getRegion());
        }

        if (region.isEmpty() && !StringUtils.isEmpty(bridgeRequestV1.getRegion())) {
            addConstraintViolation(context, CLOUD_REGION_NOT_VALID, Collections.singletonMap(REGION_PARAM, bridgeRequestV1.getRegion()), InvalidRegionException::new);
            valid = false;
        } else {
            if (!region.isEmpty()) {
                if (!region.get().isEnabled()) {
                    addConstraintViolation(context, CLOUD_REGION_NOT_ENABLED, Collections.singletonMap(REGION_PARAM, bridgeRequestV1.getRegion()), InvalidRegionException::new);
                    valid = false;
                }
            }
        }

        return valid;
    }
}
