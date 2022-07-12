package com.redhat.service.smartevents.manager.workers.resources;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.ProcessorService;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.models.Work;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class BridgeWorker extends AbstractWorker<Bridge> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeWorker.class);

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    RhoasService rhoasService;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    ProcessorService processorService;

    @Override
    protected PanacheRepositoryBase<Bridge, String> getDao() {
        return bridgeDAO;
    }

    @Override
    protected String getId(Work work) {
        // The ID of the ManagedResource to process is stored directly in the JobDetail.
        return work.getManagedResourceId();
    }

    @Override
    public Bridge createDependencies(Work work, Bridge bridge) {
        LOGGER.info("Creating dependencies for '{}' [{}]",
                bridge.getName(),
                bridge.getId());
        // Transition resource to PREPARING status.
        // PROVISIONING is handled by the Operator.
        bridge.setStatus(ManagedResourceStatus.PREPARING);

        // This is idempotent as it gets overridden later depending on actual state
        bridge.setDependencyStatus(ManagedResourceStatus.PROVISIONING);
        bridge = persist(bridge);

        // If this call throws an exception the Bridge's dependencies will be left in PROVISIONING state...
        rhoasService.createTopicAndGrantAccessFor(resourceNamesProvider.getBridgeTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // Create back-channel topic
        rhoasService.createTopicAndGrantAccessFor(resourceNamesProvider.getBridgeErrorTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // We don't need to wait for the Bridge to be READY to create the Error Handler.
        createErrorHandlerProcessor(bridge);

        bridge.setDependencyStatus(ManagedResourceStatus.READY);
        return persist(bridge);
    }

    /**
     * Creates error handler processor if required
     *
     * @param bridge input bridge
     * @return true if the work can proceed (either the error handler processor
     *         is not required or it's created and ready), false otherwise.
     */
    private void createErrorHandlerProcessor(Bridge bridge) {
        // If an ErrorHandler is not needed, consider it ready
        Action errorHandlerAction = bridge.getDefinition().getErrorHandler();
        boolean errorHandlerProcessorIsNotRequired = Objects.isNull(errorHandlerAction);
        if (errorHandlerProcessorIsNotRequired) {
            return;
        }

        String bridgeId = bridge.getId();
        String customerId = bridge.getCustomerId();
        ListResult<Processor> processors = processorService.getHiddenProcessors(bridgeId, customerId);

        // This assumes we can only have one ErrorHandler Processor per Bridge
        if (processors.getTotal() > 0) {
            return;
        }

        // create error handler processor if not present
        String errorHandlerName = String.format("Back-channel for Bridge '%s'", bridge.getId());
        ProcessorRequest errorHandlerProcessor = new ProcessorRequest(errorHandlerName, errorHandlerAction);
        processorService.createErrorHandlerProcessor(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), errorHandlerProcessor);
    }

    @Override
    protected boolean isProvisioningComplete(Bridge managedResource) {
        //As far as the Worker mechanism is concerned work for a Bridge is complete when the dependencies are complete.
        return PROVISIONING_COMPLETED.contains(managedResource.getDependencyStatus());
    }

    @Override
    public Bridge deleteDependencies(Work work, Bridge bridge) {
        LOGGER.info("Destroying dependencies for '{}' [{}]",
                bridge.getName(),
                bridge.getId());

        if (processorService.getHiddenProcessors(bridge.getId(), bridge.getCustomerId()).getTotal() > 0) {
            LOGGER.info("Hidder processors still existing for bridge '{}' [{}]. Topic deletion is delayed.", bridge.getName(), bridge.getId());
            return bridge;
        }

        LOGGER.info("Deleting topics for bridge '{}' [{}]...", bridge.getName(), bridge.getId());

        // This is idempotent as it gets overridden later depending on actual state
        bridge.setDependencyStatus(ManagedResourceStatus.DELETING);
        bridge = persist(bridge);

        // If this call throws an exception the Bridge's dependencies will be left in DELETING state...
        rhoasService.deleteTopicAndRevokeAccessFor(resourceNamesProvider.getBridgeTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // Delete back-channel topic
        rhoasService.deleteTopicAndRevokeAccessFor(resourceNamesProvider.getBridgeErrorTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // ...otherwise the Bridge's dependencies are DELETED
        bridge.setDependencyStatus(ManagedResourceStatus.DELETED);
        return persist(bridge);
    }

    @Override
    protected boolean isDeprovisioningComplete(Bridge managedResource) {
        //As far as the Worker mechanism is concerned work for a Bridge is complete when the dependencies are complete.
        return DEPROVISIONING_COMPLETED.contains(managedResource.getDependencyStatus());
    }
}
