package com.redhat.service.smartevents.manager.v2.persistence.dao;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform.DeserializationException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.manager.core.config.ConfigurationLoader;
import com.redhat.service.smartevents.manager.core.persistence.dao.AbstractCloudProviderDAO;

@V2
@ApplicationScoped
public class CloudProviderDAO extends AbstractCloudProviderDAO {

    @Inject
    public CloudProviderDAO(ConfigurationLoader configurationLoader, ObjectMapper objectMapper) {
        super(configurationLoader, objectMapper);
    }

    @Override
    @PostConstruct
    protected void init() {
        super.init();
    }

    @Override
    protected InternalPlatformException getDeserializationException(String message, Exception e) {
        return new DeserializationException(message, e);
    }

    @Override
    protected ExternalUserException getItemNotFoundException(String message) {
        return new ItemNotFoundException(message);
    }
}
