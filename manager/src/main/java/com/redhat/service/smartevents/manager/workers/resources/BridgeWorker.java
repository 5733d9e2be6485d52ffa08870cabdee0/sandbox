package com.redhat.service.smartevents.manager.workers.resources;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.ProcessorService;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ManagedResource;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.workers.Work;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class BridgeWorker extends AbstractWorker<Bridge> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeWorker.class);

    private static final String ERROR_HANDLER_NAME_TEMPLATE = "Back-channel for Bridge '%s'";

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

        // We don't need to wait for the Bridge to be READY to handle the Error Handler.
        createOrUpdateOrDeleteErrorHandlerProcessor(bridge);

        // Keep the Bridge in PREPARING until the Error Handler processor is READY.
        // This ensures Users cannot update the Bridge Error Handler until it is available.
        if (isErrorHandlerProcessorReady(bridge)) {
            bridge.setDependencyStatus(ManagedResourceStatus.READY);
        }

        return persist(bridge);
    }

    private boolean isErrorHandlerProcessorReady(Bridge bridge) {
        String bridgeId = bridge.getId();
        String customerId = bridge.getCustomerId();
        ListResult<Processor> hiddenProcessors = processorService.getHiddenProcessors(bridgeId, customerId);
        return hiddenProcessors.getItems().stream().allMatch(ManagedResource::isActionable);
    }

    /**
     * Creates, updates or deletes Error Handler processor as required
     *
     * @param bridge input bridge
     */
    private void createOrUpdateOrDeleteErrorHandlerProcessor(Bridge bridge) {
        String bridgeId = bridge.getId();
        String customerId = bridge.getCustomerId();
        ListResult<Processor> hiddenProcessors = processorService.getHiddenProcessors(bridgeId, customerId);

        // If an ErrorHandler is not defined, consider it ready and delete any lingering instances
        Action errorHandlerAction = bridge.getDefinition().getErrorHandler();
        boolean errorHandlerProcessorIsNotRequired = Objects.isNull(errorHandlerAction);
        if (errorHandlerProcessorIsNotRequired) {
            deleteErrorHandlingProcessor(bridge);
            return;
        }

        // If an Error Handler processor exists assume it is to be updated otherwise create it!
        // This assumes we can only have one ErrorHandler Processor per Bridge
        if (hiddenProcessors.getTotal() > 0) {
            // Update Error Handler processor if already present
            hiddenProcessors.getItems()
                    .stream()
                    .filter(p -> p.getType() == ProcessorType.ERROR_HANDLER)
                    .filter(ManagedResource::isActionable)
                    .findFirst()
                    .ifPresent(errorHandler -> {
                        if (errorHandler.getGeneration() < bridge.getGeneration()) {
                            String errorHandlerName = String.format(ERROR_HANDLER_NAME_TEMPLATE, bridge.getId());
                            ProcessorRequest errorHandlerProcessor = new ProcessorRequest(errorHandlerName, errorHandlerAction);
                            processorService.updateErrorHandlerProcessor(bridge.getId(), errorHandler.getId(), bridge.getCustomerId(), errorHandlerProcessor);
                        }
                    });
        } else {
            // Create Error Handler processor if not present
            String errorHandlerName = String.format(ERROR_HANDLER_NAME_TEMPLATE, bridge.getId());
            ProcessorRequest errorHandlerProcessor = new ProcessorRequest(errorHandlerName, errorHandlerAction);
            processorService.createErrorHandlerProcessor(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), errorHandlerProcessor);
        }
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

        LOGGER.info("Deleting topics for bridge '{}' [{}]...", bridge.getName(), bridge.getId());

        // This is idempotent as it gets overridden later depending on actual state
        bridge.setDependencyStatus(ManagedResourceStatus.DELETING);
        bridge = persist(bridge);

        // We don't need to wait for the Bridge to be DELETED to delete the Error Handler.
        // Delete Error Handling processor early in case there are errors deleting the topics afterwards.
        deleteErrorHandlingProcessor(bridge);

        // If this call throws an exception the Bridge's dependencies will be left in DELETING state...
        rhoasService.deleteTopicAndRevokeAccessFor(resourceNamesProvider.getBridgeTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // Delete back-channel topic
        rhoasService.deleteTopicAndRevokeAccessFor(resourceNamesProvider.getBridgeErrorTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // It's possible for the Bridge to be de-provisioned before the Error Handler processor is de-provisioned.
        // This leads to an undesirable state where we attempt to delete the Bridge record from the database
        // before the Processor record. The associated FK constraint is violated and the Bridge deletion fails.
        if (isErrorHandlerProcessorDeleted(bridge)) {
            bridge.setDependencyStatus(ManagedResourceStatus.DELETED);
        }

        return persist(bridge);
    }

    /**
     * Deletes error handler processor if required
     *
     * @param bridge input bridge
     */
    private void deleteErrorHandlingProcessor(Bridge bridge) {
        String bridgeId = bridge.getId();
        String customerId = bridge.getCustomerId();
        ListResult<Processor> hiddenProcessors = processorService.getHiddenProcessors(bridgeId, customerId);
        hiddenProcessors.getItems()
                .stream()
                .filter(p -> p.getType() == ProcessorType.ERROR_HANDLER)
                .filter(ManagedResource::isActionable)
                .forEach(p -> processorService.deleteProcessor(bridgeId, p.getId(), customerId));
    }

    private boolean isErrorHandlerProcessorDeleted(Bridge bridge) {
        String bridgeId = bridge.getId();
        String customerId = bridge.getCustomerId();
        ListResult<Processor> hiddenProcessors = processorService.getHiddenProcessors(bridgeId, customerId);
        return hiddenProcessors.getItems().isEmpty();
    }

    @Override
    protected boolean isDeprovisioningComplete(Bridge managedResource) {
        //As far as the Worker mechanism is concerned work for a Bridge is complete when the dependencies are complete.
        return DEPROVISIONING_COMPLETED.contains(managedResource.getDependencyStatus());
    }
}
