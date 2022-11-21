package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.shard.operator.reconcilers.BridgeReconciler;
import com.redhat.service.smartevents.shard.operator.reconcilers.ProcessorReconciler;
import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ManagerSyncServiceImpl implements ManagerSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSyncServiceImpl.class);

    @Inject
    BridgeReconciler bridgeReconciler;

    @Inject
    ProcessorReconciler processorReconciler;

    @Override
    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void syncUpdatesFromManager() {
        LOGGER.debug("Fetching updates from Manager for Bridges and Processors to deploy and delete");
        bridgeReconciler.reconcile();
        processorReconciler.reconcile();
    }
}
