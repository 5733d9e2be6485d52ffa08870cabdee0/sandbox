package com.redhat.service.smartevents.shard.operator.reconcilers;

import com.redhat.service.smartevents.shard.operator.BridgeIngressService;
import com.redhat.service.smartevents.shard.operator.BridgeIngressServiceImpl;
import com.redhat.service.smartevents.shard.operator.DeltaProcessorService;
import com.redhat.service.smartevents.shard.operator.ManagerClient;
import com.redhat.service.smartevents.shard.operator.comparators.BridgeIngressComparator;
import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.converters.BridgeIngressConverter;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class BridgeReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeIngressServiceImpl.class);

    @Inject
    ManagerClient managerClient;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    BridgeIngressConverter bridgeIngressConverter;

    @Inject
    DeltaProcessorService deltaProcessorService;

    public void reconcile(){

        Uni<List<BridgeIngress>> requestedResources = createRequiredResources();

        Uni<List<BridgeIngress>> deployedResources = fetchDeployedResources();

        processDelta(requestedResources, deployedResources);
    }

    private Uni<List<BridgeIngress>> createRequiredResources() {
        return managerClient.fetchBridgesToDeployOrDelete().onItem().transform(bridges -> bridges.stream().map(bridgeDTO -> bridgeIngressConverter.fromBridgeDTOToBridgeIngress(bridgeDTO)).collect(Collectors.toList()));
    }

    private Uni<List<BridgeIngress>> fetchDeployedResources() {
        return Uni.createFrom().item(bridgeIngressService.fetchAllBridgeIngress());
    }

    private void processDelta(Uni<List<BridgeIngress>> requestedResources, Uni<List<BridgeIngress>> deployedResources) {
        Uni.combine().all().unis(requestedResources, deployedResources).asTuple().onItem().invoke(tuple -> {
            Comparator<BridgeIngress> bridgeIngressComparator = new BridgeIngressComparator();
            boolean deltaProcessed = deltaProcessorService.processDelta(BridgeIngress.class, bridgeIngressComparator, tuple.getItem1(), tuple.getItem2());
            if (deltaProcessed) {
                LOGGER.info("Delta has been processed successfully");
            }
        }).subscribe().with(
                success -> LOGGER.info("Bridge reconcile success"),
                failure -> LOGGER.info("Bridge reconcile failed")
        );
    }
}
