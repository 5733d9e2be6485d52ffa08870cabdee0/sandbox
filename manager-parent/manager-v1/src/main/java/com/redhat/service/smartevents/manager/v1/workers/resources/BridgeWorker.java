package com.redhat.service.smartevents.manager.v1.workers.resources;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.infra.v1.api.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.manager.core.dns.DnsService;
import com.redhat.service.smartevents.manager.core.providers.GlobalResourceNamesProvider;
import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.v1.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v1.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v1.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v1.services.BridgesService;
import com.redhat.service.smartevents.manager.v1.services.ProcessorService;
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
    GlobalResourceNamesProvider globalResourceNamesProvider;

    @Inject
    BridgesService bridgesService;

    @Inject
    ProcessorService processorService;

    @Inject
    DnsService dnsService;

    @Override
    public PanacheRepositoryBase<Bridge, String> getDao() {
        return bridgeDAO;
    }

    @Override
    public String getId(Work work) {
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
        bridge.setStatus(ManagedResourceStatusV1.PREPARING);

        // This is idempotent as it gets overridden later depending on actual state
        bridge.setDependencyStatus(ManagedResourceStatusV1.PROVISIONING);
        bridge = persist(bridge);

        // If this call throws an exception the Bridge's dependencies will be left in PROVISIONING state...
        rhoasService.createTopicAndGrantAccessFor(globalResourceNamesProvider.getBridgeTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // Create back-channel topic
        rhoasService.createTopicAndGrantAccessFor(globalResourceNamesProvider.getBridgeErrorTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // We don't need to wait for the Bridge to be READY to handle the Error Handler.
        createOrUpdateOrDeleteErrorHandlerProcessor(bridge);

        // Create DNS record
        dnsService.createDnsRecord(bridge.getId());

        Optional<Processor> optErrorHandler = processorService.getErrorHandler(bridge.getId(), bridge.getCustomerId());
        if (optErrorHandler.isEmpty()) {
            // If an Error Handler isn't required there are no dependencies.
            bridge.setDependencyStatus(ManagedResourceStatusV1.READY);
        } else {
            Processor errorHandler = optErrorHandler.get();
            if (errorHandler.getStatus() == ManagedResourceStatusV1.READY) {
                // Keep the Bridge in PREPARING until the Error Handler processor is READY (or FAILED).
                // This ensures Users cannot update the Bridge Error Handler until it is available.
                bridge.setDependencyStatus(ManagedResourceStatusV1.READY);
            } else if (errorHandler.getStatus() == ManagedResourceStatusV1.FAILED) {
                return persistAndPropagateErrorHandlerFailure(bridge, errorHandler);
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
                .ifPresentOrElse(errorHandler -> {
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
    public boolean isProvisioningComplete(Bridge managedResource) {
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
        bridge.setDependencyStatus(ManagedResourceStatusV1.DELETING);
        bridge = persist(bridge);

        // We don't need to wait for the Bridge to be DELETED to delete the Error Handler.
        // Delete Error Handling processor early in case there are errors deleting the topics afterwards.
        deleteErrorHandlingProcessor(bridge);

        // If this call throws an exception the Bridge's dependencies will be left in DELETING state...
        rhoasService.deleteTopicAndRevokeAccessFor(globalResourceNamesProvider.getBridgeTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // Delete back-channel topic
        rhoasService.deleteTopicAndRevokeAccessFor(globalResourceNamesProvider.getBridgeErrorTopicName(bridge.getId()),
                RhoasTopicAccessType.CONSUMER_AND_PRODUCER);

        // Delete DNS entry
        dnsService.deleteDnsRecord(bridge.getId());

        // It's possible for the Bridge to be de-provisioned before the Error Handler processor is de-provisioned.
        // This leads to an undesirable state where we attempt to delete the Bridge record from the database
        // before the Processor record. The associated FK constraint is violated and the Bridge deletion fails.
        // Therefore, don't mark the Bridge ready for deletion (by the Operator) until the Error Handler is removed.
        Optional<Processor> optErrorHandler = processorService.getErrorHandler(bridge.getId(), bridge.getCustomerId());
        if (optErrorHandler.isEmpty()) {
            bridge.setDependencyStatus(ManagedResourceStatusV1.DELETED);
        } else {
            Processor errorHandler = optErrorHandler.get();
            if (errorHandler.getStatus() == ManagedResourceStatusV1.FAILED) {
                return persistAndPropagateErrorHandlerFailure(bridge, errorHandler);
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
                .ifPresent(errorHandler -> processorService.deleteProcessor(bridgeId, errorHandler.getId(), customerId));
    }

    @Override
    public boolean isDeprovisioningComplete(Bridge managedResource) {
        //As far as the Worker mechanism is concerned work for a Bridge is complete when the dependencies are complete.
        return DEPROVISIONING_COMPLETED.contains(managedResource.getDependencyStatus());
    }

    // The ErrorHandler Processor is a special type. It is not visible to Users and hence
    // should it fail to be provisioned (or de-provisioned) the User is unaware of the reason.
    // Therefore, propagate the error associated with the Error Handler Processor to the Bridge.
    protected Bridge persistAndPropagateErrorHandlerFailure(Bridge bridge, Processor errorHandler) {
        bridge.setErrorId(errorHandler.getErrorId());
        bridge.setErrorUUID(errorHandler.getErrorUUID());
        bridge.setDependencyStatus(ManagedResourceStatusV1.FAILED);
        persist(bridge);

        // Updating the status through the service ensures metrics are correctly handled.
        BridgeErrorInstance bei = bridgeErrorHelper.getBridgeErrorInstance(errorHandler.getErrorId(), errorHandler.getErrorUUID());
        return bridgesService.updateBridgeStatus(new ManagedResourceStatusUpdateDTO(bridge.getId(), bridge.getCustomerId(), ManagedResourceStatusV1.FAILED, bei));
    }

}
