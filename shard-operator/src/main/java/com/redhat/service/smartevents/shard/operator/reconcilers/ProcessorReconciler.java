package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.BridgeExecutorService;
import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.ManagerClient;
import com.redhat.service.smartevents.shard.operator.comparators.BridgeExecutorComparator;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.converters.BridgeExecutorConverter;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProcessorReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorReconciler.class);

    @Inject
    ManagerClient managerClient;

    @Inject
    BridgeExecutorService bridgeExecutorService;

    @Inject
    DeltaProcessorService deltaProcessorService;

    @Inject
    BridgeExecutorConverter bridgeExecutorConverter;

    public void reconcile(){

        Uni<List<BridgeExecutor>> requestedResources = createRequiredResources();

        Uni<List<BridgeExecutor>> deployedResources = fetchDeployedResources();

        processDelta(requestedResources, deployedResources);
    }

    private Uni<List<BridgeExecutor>> createRequiredResources() {
        return managerClient.fetchProcessorsToDeployOrDelete().onItem().transform(processors -> processors.stream().map(p -> bridgeExecutorConverter.fromProcessorDTOToBridgeExecutor(p)).collect(Collectors.toList()));
    }

    private Uni<List<BridgeExecutor>> fetchDeployedResources() {
        return Uni.createFrom().item(bridgeExecutorService.fetchAllBridgeExecutor());
    }

    private void processDelta(Uni<List<BridgeExecutor>> requestedResources, Uni<List<BridgeExecutor>> deployedResources) {
        Uni.combine().all().unis(requestedResources, deployedResources).asTuple().onItem().invoke(tuple -> {
            Comparator<BridgeExecutor> bridgeExecutorComparator = new BridgeExecutorComparator();
            boolean deltaProcessed = deltaProcessorService.processDelta(BridgeExecutor.class, bridgeExecutorComparator, tuple.getItem1(), tuple.getItem2());
            if (deltaProcessed) {
                LOGGER.info("Delta has been processed successfully");
            }
        }).subscribe().with(
                success -> LOGGER.info("Processor reconcile success"),
                failure -> LOGGER.info("Processor reconcile failed")
        );
    }
}
