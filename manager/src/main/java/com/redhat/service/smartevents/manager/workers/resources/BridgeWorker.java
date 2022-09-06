package com.redhat.service.smartevents.manager.workers.resources;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.ProcessorService;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.dns.DnsService;
import com.redhat.service.smartevents.manager.models.Bridge;
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

    @Inject
    DnsService dnsService;

    @Inject
    BridgeErrorHelper bridgeErrorHelper;

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

        // Create DNS record
        dnsService.createDnsRecord(bridge.getId());

        Optional<Processor> optErrorHandler = processorService.getErrorHandler(bridge.getId(), bridge.getCustomerId());
        if (optErrorHandler.isEmpty()) {
            // If an Error Handler isn't required there are no dependencies.
            bridge.setDependencyStatus(ManagedResourceStatus.READY);
        } else {
            Processor errorHandler = optErrorHandler.get();
            if (errorHandler.getStatus() == ManagedResourceStatus.READY) {
                // Keep the Bridge in PREPARING until the Error Handler processor is READY (or FAILED).
                // This ensures Users cannot update the Bridge Error Handler until it is available.
                bridge.setDependencyStatus(ManagedResourceStatus.READY);
            } else if (errorHandler.getStatus() == ManagedResourceStatus.FAILED) {
                bridge.setStatus(ManagedResourceStatus.FAILED);
                bridge.setDependencyStatus(ManagedResourceStatus.FAILED);
                propagateProcessorError(bridge);
            }
        }

        return persist(bridge);
    }

    /**
     * Creates, updates or deletes Error Handler processor as required
     *
     * @param bridge input bridge
     */
    private void createOrUpdateOrDeleteErrorHandlerProcessor(Bridge bridge) {
        // If an ErrorHandler is not defined, consider it ready and delete any lingering instances
        Action errorHandlerAction = bridge.getDefinition().getResolvedErrorHandler();
        boolean errorHandlerProcessorIsNotRequired = Objects.isNull(errorHandlerAction);
        if (errorHandlerProcessorIsNotRequired) {
            deleteErrorHandlingProcessor(bridge);
            return;
        }

        // If an Error Handler processor exists assume it is to be updated otherwise create it!
        processorService
                .getErrorHandler(bridge.getId(), bridge.getCustomerId())
                .ifPresentOrElse((errorHandler) -> {
                    if (errorHandler.getGeneration() < bridge.getGeneration()) {
                        String errorHandlerName = String.format(ERROR_HANDLER_NAME_TEMPLATE, bridge.getId());
                        ProcessorRequest errorHandlerProcessor = new ProcessorRequest(errorHandlerName, errorHandlerAction);
                        processorService.updateErrorHandlerProcessor(bridge.getId(), errorHandler.getId(), bridge.getCustomerId(), errorHandlerProcessor);
                    }
                },
                        () -> {
                            // Create Error Handler processor if not present
                            String errorHandlerName = String.format(ERROR_HANDLER_NAME_TEMPLATE, bridge.getId());
                            ProcessorRequest errorHandlerProcessor = new ProcessorRequest(errorHandlerName, errorHandlerAction);
                            processorService.createErrorHandlerProcessor(bridge.getId(), bridge.getCustomerId(), bridge.getOwner(), errorHandlerProcessor);
                        });
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

        // Delete DNS entry
        dnsService.deleteDnsRecord(bridge.getId());

        // It's possible for the Bridge to be de-provisioned before the Error Handler processor is de-provisioned.
        // This leads to an undesirable state where we attempt to delete the Bridge record from the database
        // before the Processor record. The associated FK constraint is violated and the Bridge deletion fails.
        // Therefore, don't mark the Bridge ready for deletion (by the Operator) until the Error Handler is removed.
        Optional<Processor> optErrorHandler = processorService.getErrorHandler(bridge.getId(), bridge.getCustomerId());
        if (optErrorHandler.isEmpty()) {
            bridge.setDependencyStatus(ManagedResourceStatus.DELETED);
        } else {
            Processor errorHandler = optErrorHandler.get();
            if (errorHandler.getStatus() == ManagedResourceStatus.FAILED) {
                bridge.setStatus(ManagedResourceStatus.FAILED);
                bridge.setDependencyStatus(ManagedResourceStatus.FAILED);
                propagateProcessorError(bridge);
            }
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
        processorService
                .getErrorHandler(bridgeId, customerId)
                .ifPresent((errorHandler) -> processorService.deleteProcessor(bridgeId, errorHandler.getId(), customerId));
    }

    @Override
    protected boolean isDeprovisioningComplete(Bridge managedResource) {
        //As far as the Worker mechanism is concerned work for a Bridge is complete when the dependencies are complete.
        return DEPROVISIONING_COMPLETED.contains(managedResource.getDependencyStatus());
    }

    @Override
    protected Bridge recordError(Work work, Exception e) {
        String bridgeId = work.getManagedResourceId();
        Bridge bridge = getDao().findById(bridgeId);
        BridgeErrorInstance bridgeErrorInstance = bridgeErrorHelper.getBridgeErrorInstance(e);
        bridge.setErrorId(bridgeErrorInstance.getId());
        bridge.setErrorUUID(bridgeErrorInstance.getUuid());
        return persist(bridge);
    }

    // The ErrorHandler Processor is a special type. It is not visible to Users and hence
    // should it fail to be provisioned (or de-provisioned) the User is unaware of the reason.
    // Therefore, propagate the error associated with the Error Handler Processor to the Bridge.
    protected void propagateProcessorError(Bridge bridge) {
        String bridgeId = bridge.getId();
        String customerId = bridge.getCustomerId();
        processorService.getErrorHandler(bridgeId, customerId)
                .ifPresent(errorHandler -> {
                    bridge.setErrorId(errorHandler.getErrorId());
                    bridge.setErrorUUID(errorHandler.getErrorUUID());
                    persist(bridge);
                });
    }

}
