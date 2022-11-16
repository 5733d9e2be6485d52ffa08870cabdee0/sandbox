package com.redhat.service.smartevents.shard.operator.v1.utils;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBrokerConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBrokerStatus;
import com.redhat.service.smartevents.shard.operator.core.utils.DeploymentStatusUtils;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.v1.utils.networking.NetworkingTestUtils;

import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceStatusBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.apps.DeploymentConditionBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationScoped
public class KubernetesResourcePatcher {

    @ConfigProperty(name = "event-bridge.istio.gateway.name")
    String gatewayName;

    @ConfigProperty(name = "event-bridge.istio.gateway.namespace")
    String gatewayNamespace;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NetworkingTestUtils networkingTestUtils;

    private static final String FAILURE_MESSAGE = "The deployment has failed";

    private static final String FAILURE_REASON = "You were too optimistic";

    public void cleanUp() {
        kubernetesClient.resources(BridgeIngress.class).inAnyNamespace().delete();
        kubernetesClient.resources(BridgeExecutor.class).inAnyNamespace().delete();
        kubernetesClient.secrets().inAnyNamespace().delete();
        kubernetesClient.apps().deployments().inAnyNamespace().delete();
        kubernetesClient.services().inAnyNamespace().delete();
        networkingTestUtils.cleanUp();
    }

    private Deployment getDeployment(String name, String namespace) {
        Deployment deployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();

        // Fail if it has not been deployed yet
        assertThat(deployment).withFailMessage("Deployment with name '%s' in namespace '%s' does not exist.", name, namespace)
                .isNotNull();
        return deployment;
    }

    private void updateDeploymentStatus(Deployment deployment, DeploymentStatus deploymentStatus) {
        deployment.setStatus(deploymentStatus);
        kubernetesClient.apps().deployments()
                .inNamespace(deployment.getMetadata().getNamespace())
                .withName(deployment.getMetadata().getName())
                .replace(deployment);
    }

    public void patchDeploymentAsReplicaFailed(String name, String namespace) {
        Deployment deployment = getDeployment(name, namespace);

        DeploymentCondition deploymentCondition = new DeploymentConditionBuilder()
                .withType(DeploymentStatusUtils.REPLICA_FAILURE_CONDITION_TYPE)
                .withStatus(DeploymentStatusUtils.STATUS_TRUE)
                .withReason(FAILURE_REASON)
                .withMessage(FAILURE_MESSAGE)
                .build();

        DeploymentStatus deploymentStatus = new DeploymentStatusBuilder().withReplicas(1).withUnavailableReplicas(1).withAvailableReplicas(0).withConditions(deploymentCondition).build();
        updateDeploymentStatus(deployment, deploymentStatus);
    }

    public void patchDeploymentAsTimeoutFailed(String name, String namespace) {
        Deployment deployment = getDeployment(name, namespace);

        DeploymentCondition deploymentCondition = new DeploymentConditionBuilder()
                .withType(DeploymentStatusUtils.PROGRESSING_CONDITION_TYPE)
                .withStatus(DeploymentStatusUtils.STATUS_FALSE)
                .withReason(DeploymentStatusUtils.PROGRESS_DEADLINE_EXCEEDED_CONDITION_REASON)
                .withMessage(FAILURE_MESSAGE)
                .build();

        DeploymentStatus deploymentStatus = new DeploymentStatusBuilder().withReplicas(1).withUnavailableReplicas(1).withAvailableReplicas(0).withConditions(deploymentCondition).build();
        updateDeploymentStatus(deployment, deploymentStatus);
    }

    public void patchReadyDeploymentAsReady(String name, String namespace) {
        Deployment deployment = getDeployment(name, namespace);

        // Fail if it has not been deployed yet
        assertThat(deployment).isNotNull();

        DeploymentStatus deploymentStatus = new DeploymentStatusBuilder().withAvailableReplicas(1).withReplicas(1).build();
        updateDeploymentStatus(deployment, deploymentStatus);
    }

    public void patchReadyKnativeBroker(String name, String namespace) {
        KnativeBrokerStatus.Address address = new KnativeBrokerStatus.Address();
        address.setUrl("http://kafka-broker-ingress.knative-eventing.svc.cluster.local/ob-55029811/ob-bridgesdeployed-1");

        KnativeBrokerStatus knativeBrokerStatus = new KnativeBrokerStatus();
        knativeBrokerStatus.setAddress(address);
        knativeBrokerStatus.setConditions(Collections.singleton(new Condition(KnativeBrokerConditionTypeConstants.ADDRESSABLE, ConditionStatus.True)));

        KnativeBroker knativeBroker = kubernetesClient
                .resources(KnativeBroker.class)
                .inNamespace(namespace)
                .withName(name)
                .get();

        assertThat(knativeBroker).isNotNull();

        knativeBroker.setStatus(knativeBrokerStatus);

        kubernetesClient
                .resources(KnativeBroker.class)
                .inNamespace(namespace)
                .withName(name)
                .replace(knativeBroker);
    }

    public void patchReadyService(String name, String namespace) {
        // Retrieve the service
        Service service = kubernetesClient.services()
                .inNamespace(namespace)
                .withName(name)
                .get();

        // Fail if it has not been deployed yet
        assertThat(service).isNotNull();

        // Patch the service - This is what k8s would do when the resource is deployed and is ready.
        service.setStatus(new ServiceStatusBuilder().withLoadBalancer(new LoadBalancerStatus()).build());
        kubernetesClient.services()
                .inNamespace(namespace)
                .withName(name)
                .replace(service);
    }

    public void patchReadyNetworkResource(String name, String namespace) {
        // Retrieve the network resource
        Namespaced resource = networkingTestUtils.getNetworkResource(name, namespace);

        // Fail if it has not been deployed yet
        assertThat(resource).isNotNull();

        // Patch the network resource - This is what k8s would do when the resource is deployed and is ready.
        networkingTestUtils.patchNetworkResource(name, namespace);
    }
}
