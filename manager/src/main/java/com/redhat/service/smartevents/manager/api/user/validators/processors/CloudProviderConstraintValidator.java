package com.redhat.service.smartevents.manager.api.user.validators.processors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.dao.CloudProviderDAO;
import com.redhat.service.smartevents.manager.models.CloudProvider;

@ApplicationScoped
public class CloudProviderConstraintValidator extends BaseConstraintValidator<ValidCloudProvider, BridgeRequest> {

    static final String CLOUD_PROVIDER_NOT_VALID = "Cloud Provider '{id}' is not valid.";

    static final String CLOUD_PROVIDER_NOT_ENABLED = "Cloud Provider '{id}' is not enabled";

    static final String CLOUD_REGION_NOT_VALID = "The Region '{region}' for Cloud Provider '{id}' is not valid.";

    static final String CLOUD_REGION_NOT_ENABLED = "The Region '{region}' for Cloud Provider '{id}' is not enabled.";

    @Inject
    CloudProviderDAO cloudProviderDAO;

    @Override
    public void initialize(ValidCloudProvider constraintAnnotation) {
        super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(BridgeRequest bridgeRequest, ConstraintValidatorContext context) {
        CloudProvider cloudProvider = cloudProviderDAO.findById(bridgeRequest.getCloudProvider());
        if(cloudProvider == null) {
            addConstraintViolation();
        }
        return true;
    }
}
