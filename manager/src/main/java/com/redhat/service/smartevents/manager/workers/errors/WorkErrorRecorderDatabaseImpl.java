package com.redhat.service.smartevents.manager.workers.errors;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.manager.dao.WorkErrorDAO;
import com.redhat.service.smartevents.manager.models.WorkError;

@ApplicationScoped
public class WorkErrorRecorderDatabaseImpl implements WorkErrorRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkErrorRecorderDatabaseImpl.class);

    @Inject
    WorkErrorDAO workErrorDAO;

    @Inject
    BridgeErrorHelper bridgeErrorHelper;

    @Override
    @Transactional
    public void deleteErrors(String managedResourceId) {
        workErrorDAO.deleteByManagedResourceId(managedResourceId);
    }

    @Override
    public void recordError(String managedResourceId, Exception e) {
        BridgeError bridgeError = bridgeErrorHelper.getBridgeError(e);
        recordError(managedResourceId, bridgeError);
    }

    @Override
    @Transactional
    public void recordError(String managedResourceId, BridgeError bridgeError) {
        LOGGER.info("Persisting BridgeError '{}' for Managed Resource '{}'.", bridgeError.getCode(), managedResourceId);
        workErrorDAO.persist(new WorkError(managedResourceId,
                bridgeError.getCode(),
                bridgeError.getReason(),
                bridgeError.getType(),
                ZonedDateTime.now(ZoneOffset.UTC)));
    }

}
