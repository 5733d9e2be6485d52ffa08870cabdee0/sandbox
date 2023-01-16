package com.redhat.service.smartevents.manager.v1.api.validators;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user.InvalidCloudProviderException;
import com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user.InvalidRegionException;
import com.redhat.service.smartevents.manager.core.api.validators.AbstractCloudProviderConstraintValidator;
import com.redhat.service.smartevents.manager.core.persistence.dao.CloudProviderDAO;

@ApplicationScoped
public class CloudProviderConstraintValidator extends AbstractCloudProviderConstraintValidator<ValidCloudProvider> {

    public CloudProviderConstraintValidator() {
        //CDI proxy
    }

    @Inject
    public CloudProviderConstraintValidator(@V1 CloudProviderDAO cloudProviderDAO) {
        super(cloudProviderDAO);
    }

    @Override
    protected ExternalUserException getInvalidCloudProviderException(String message) {
        return new InvalidCloudProviderException(message);
    }

    @Override
    protected ExternalUserException getInvalidRegionException(String message) {
        return new InvalidRegionException(message);
    }
}
