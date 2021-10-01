package com.developer.service.bridge.k8s;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.executor.ExecutorsK8SDeploymentManager;
import com.redhat.service.bridge.infra.k8s.Action;
import com.redhat.service.bridge.infra.k8s.K8SBridgeConstants;
import com.redhat.service.bridge.infra.k8s.KubernetesResourceType;
import com.redhat.service.bridge.infra.k8s.ResourceEvent;
import com.redhat.service.bridge.infra.k8s.crds.ProcessorCustomResource;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentConditionBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder;

@ApplicationScoped
public class DeploymentsManagerImpl implements DeploymentsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentsManagerImpl.class);

    private final Map<String, Deployment> deploymentMap = new HashMap<>();

    @Inject
    Event<ResourceEvent> event;

    @Inject
    ExecutorsK8SDeploymentManager executorsK8SDeploymentManager;

    @Inject
    CustomResourceManager customResourceManager;

    @Override
    public void createOrUpdate(Deployment deployment) {
        String name = deployment.getMetadata().getName();
        Action action;
        if (deploymentMap.containsKey(name)) {
            deploymentMap.replace(name, deployment);
            action = Action.MODIFIED;
        } else {
            deploymentMap.put(name, deployment);
            action = Action.ADDED;
        }

        DeploymentStatus status = new DeploymentStatusBuilder()
                .withConditions(new DeploymentConditionBuilder().withStatus("Ready").build())
                .build();
        deployment.setStatus(status);

        String type = KubernetesUtils.extractLabelFromMetadata(deployment, K8SBridgeConstants.METADATA_TYPE);
        if (type.equals(K8SBridgeConstants.PROCESSOR_TYPE)) {
            // hack for the time being
            ProcessorCustomResource processorCustomResource = customResourceManager.getCustomResource(name, ProcessorCustomResource.class);
            try {
                executorsK8SDeploymentManager.deploy(processorCustomResource.toDTO());
            } catch (Exception e) {
                LOGGER.error("Failed to deploy Executor for Processor '{}' on Bridge '{}'", processorCustomResource.getId(), processorCustomResource.getBridge().getId(), e);
                action = Action.ERROR;
            }
        }
        if (type.equals(K8SBridgeConstants.BRIDGE_TYPE)) {
            LOGGER.debug("[k8s] New deployment for Ingress Bridge, but it will be available only when the Ingress/Route will be exposed.");
        }

        event.fire(new ResourceEvent(KubernetesResourceType.DEPLOYMENT, type, name, action));
    }

    @Override
    public void delete(String id) {
        if (deploymentMap.containsKey(id)) {
            Deployment deployment = deploymentMap.get(id);
            if (KubernetesUtils.extractLabelFromMetadata(deployment, K8SBridgeConstants.METADATA_TYPE).equals(K8SBridgeConstants.PROCESSOR_TYPE)) {
                String bridgeId = KubernetesUtils.extractLabelFromMetadata(deployment, K8SBridgeConstants.METADATA_BRIDGE_ID);
                executorsK8SDeploymentManager.undeploy(bridgeId, id);
            }
            deploymentMap.remove(id);
            event.fire(new ResourceEvent(KubernetesResourceType.DEPLOYMENT, KubernetesUtils.extractLabelFromMetadata(deployment, K8SBridgeConstants.METADATA_TYPE), id, Action.DELETED));
        }
    }

    @Override
    public Deployment getDeployment(String name) {
        return deploymentMap.get(name);
    }
}
