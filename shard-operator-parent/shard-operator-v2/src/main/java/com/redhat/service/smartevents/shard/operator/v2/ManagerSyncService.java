package com.redhat.service.smartevents.shard.operator.v2;

import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ManagerSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSyncService.class);

    @Inject
    ManagedBridgeSyncService managedBridgeSyncService;

    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void syncUpdatesFromManager() {
        LOGGER.debug("Fetching updates from Manager for Bridges to deploy and delete");
        managedBridgeSyncService.syncManagedBridgeWithManager();
    }
}
