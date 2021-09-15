package com.redhat.service.bridge.shard;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.dto.BridgeDTO;
import com.redhat.service.bridge.infra.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.k8s.K8SBridgeConstants;
import com.redhat.service.bridge.infra.k8s.KubernetesClient;
import com.redhat.service.bridge.infra.k8s.crds.BridgeCustomResource;
import com.redhat.service.bridge.infra.k8s.crds.ProcessorCustomResource;

@ApplicationScoped
public class OperatorServiceInMemoryImpl implements OperatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorServiceInMemoryImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public BridgeDTO createBridgeDeployment(BridgeDTO bridge) {
        LOGGER.info("[shard] Processing deployment of Bridge with id '{}' and name '{}' for customer '{}'",
                bridge.getId(), bridge.getName(), bridge.getCustomerId());

        // Create the custom resource, and let the controller create what it needs
        kubernetesClient.createOrUpdateCustomResource(bridge.getId(), BridgeCustomResource.fromDTO(bridge), K8SBridgeConstants.BRIDGE_TYPE);
        return bridge;
    }

    @Override
    public BridgeDTO deleteBridgeDeployment(BridgeDTO bridge) {
        // Delete the custom resource, and let the controller delete what the resources
        kubernetesClient.deleteCustomResource(bridge.getId(), K8SBridgeConstants.BRIDGE_TYPE);
        LOGGER.info("[shard] Bridge with id '{}' and name '{}' for customer '{}' has been deleted",
                bridge.getId(), bridge.getName(), bridge.getCustomerId());
        return bridge;
    }

    @Override
    public ProcessorDTO createProcessorDeployment(ProcessorDTO processor) {
        LOGGER.info("[shard] Processing deployment of Processor with id '{}' and name '{}' for customer '{}'",
                processor.getId(), processor.getName(), processor.getBridge().getCustomerId());

        // Create the custom resource, and let the controller create what it needs
        kubernetesClient.createOrUpdateCustomResource(processor.getId(), ProcessorCustomResource.fromDTO(processor), K8SBridgeConstants.PROCESSOR_TYPE);
        return processor;
    }
}
