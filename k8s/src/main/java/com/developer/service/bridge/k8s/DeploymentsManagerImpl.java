package com.developer.service.bridge.k8s;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.executor.ExecutorsService;
import com.redhat.service.bridge.infra.k8s.Action;
import com.redhat.service.bridge.infra.k8s.K8SBridgeConstants;
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
    ExecutorsService executorsService;

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

        String type = KubernetesUtils.extractTypeFromMetadata(deployment);
        if (type.equals(K8SBridgeConstants.PROCESSOR_TYPE)) {
            // hack for the time being
            ProcessorCustomResource processorCustomResource = customResourceManager.getCustomResource(name, ProcessorCustomResource.class);
            executorsService.createExecutor(processorCustomResource.toDTO());
        }
        if (type.equals(K8SBridgeConstants.BRIDGE_TYPE)) {
            LOGGER.debug("[k8s] New deployment for Ingress Bridge, but it will be available only when the Ingress/Route will be exposed.");
        }

        event.fire(new ResourceEvent(type, name, action));
    }

    @Override
    public void delete(String name) {
        if (deploymentMap.containsKey(name)) {
            Deployment deployment = deploymentMap.get(name);
            deploymentMap.remove(name);
            event.fire(new ResourceEvent(KubernetesUtils.extractTypeFromMetadata(deployment), name, Action.DELETED));
        }
    }

    @Override
    public Deployment getDeployment(String name) {
        return deploymentMap.get(name);
    }
}
