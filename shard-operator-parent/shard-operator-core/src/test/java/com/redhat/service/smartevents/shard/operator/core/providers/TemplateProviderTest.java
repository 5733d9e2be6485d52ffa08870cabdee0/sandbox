package com.redhat.service.smartevents.shard.operator.core.providers;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.shard.operator.core.networking.NetworkingConstants;
import com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy.AuthorizationPolicy;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.Route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TemplateProviderTest {

    private static final String RESOURCE_NAME = "my-test-name";

    private static final String KIND = "ManagedBridge";

    private static final String API_VERSION = "v2alpha1";

    private static final String NAMESPACE = "my-bridge-namespace";

    private static final String BRIDGE_COMPONENT_NAME = "ingress";

    private static final String EXECUTOR_COMPONENT_NAME = "executor";

    @Mock
    private HasMetadata hasMetadata;

    @Mock
    private ObjectMeta objectMeta;

    private void mockResourceForNoOwnerReference() {
        when(hasMetadata.getMetadata()).thenReturn(objectMeta);
        when(objectMeta.getName()).thenReturn(RESOURCE_NAME);
        when(objectMeta.getNamespace()).thenReturn(NAMESPACE);
    }

    private void mockResourceForOwnerReference() {
        when(hasMetadata.getApiVersion()).thenReturn(API_VERSION);
        when(hasMetadata.getKind()).thenReturn(KIND);

        when(hasMetadata.getMetadata()).thenReturn(objectMeta);
        when(objectMeta.getName()).thenReturn(RESOURCE_NAME);
        when(objectMeta.getNamespace()).thenReturn(NAMESPACE);
        when(objectMeta.getUid()).thenReturn(UUID.randomUUID().toString());
    }

    @Test
    public void metadataIsUpdated() {

        mockResourceForOwnerReference();

        TemplateProvider templateProvider = new TemplateProviderImpl();
        Deployment deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, TemplateImportConfig.withDefaults(LabelsBuilder.V1_OPERATOR_NAME));
        assertOwnerReference(hasMetadata, deployment.getMetadata());
        assertThat(deployment.getMetadata().getName()).isEqualTo(hasMetadata.getMetadata().getName());
        assertThat(deployment.getMetadata().getNamespace()).isEqualTo(hasMetadata.getMetadata().getNamespace());

        deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, new TemplateImportConfig(LabelsBuilder.V1_OPERATOR_NAME).withNameFromParent());
        assertThat(deployment.getMetadata().getOwnerReferences()).isNull();
        assertThat(deployment.getMetadata().getName()).isEqualTo(hasMetadata.getMetadata().getName());
        assertThat(deployment.getMetadata().getNamespace()).isNull();

        deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, new TemplateImportConfig(LabelsBuilder.V1_OPERATOR_NAME).withNamespaceFromParent());
        assertThat(deployment.getMetadata().getOwnerReferences()).isNull();
        assertThat(deployment.getMetadata().getName()).isNull();
        assertThat(deployment.getMetadata().getNamespace()).isEqualTo(hasMetadata.getMetadata().getNamespace());

        deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, new TemplateImportConfig(LabelsBuilder.V1_OPERATOR_NAME).withOwnerReferencesFromParent());
        assertOwnerReference(hasMetadata, deployment.getMetadata());
        assertThat(deployment.getMetadata().getName()).isNull();
        assertThat(deployment.getMetadata().getNamespace()).isNull();
    }

    @Test
    public void bridgeExecutorDeploymentTemplateIsProvided() {

        mockResourceForOwnerReference();
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Deployment deployment = templateProvider.loadBridgeExecutorDeploymentTemplate(hasMetadata, TemplateImportConfig.withDefaults(LabelsBuilder.V1_OPERATOR_NAME));

        assertOwnerReference(hasMetadata, deployment.getMetadata());
        assertLabels(deployment.getMetadata(), LabelsBuilder.V1_OPERATOR_NAME, EXECUTOR_COMPONENT_NAME);
        assertThat(deployment.getSpec().getReplicas()).isEqualTo(1);
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getLivenessProbe()).isNotNull();
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getReadinessProbe()).isNotNull();
    }

    @Test
    public void bridgeExecutorServiceTemplateIsProvided() {
        mockResourceForOwnerReference();

        TemplateProvider templateProvider = new TemplateProviderImpl();
        Service service = templateProvider.loadBridgeExecutorServiceTemplate(hasMetadata, TemplateImportConfig.withDefaults(LabelsBuilder.V1_OPERATOR_NAME));

        assertOwnerReference(hasMetadata, service.getMetadata());
        assertLabels(service.getMetadata(), LabelsBuilder.V1_OPERATOR_NAME, EXECUTOR_COMPONENT_NAME);
        assertThat(service.getSpec().getPorts().size()).isEqualTo(1);
        assertThat(service.getSpec().getPorts().get(0).getName()).isEqualTo("web");
        assertThat(service.getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getTargetPort().getIntVal()).isEqualTo(8080);
        assertThat(service.getSpec().getPorts().get(0).getProtocol()).isEqualTo("TCP");
    }

    @Test
    public void bridgeIngressBrokerTemplateIsProvided() {
        mockResourceForOwnerReference();
        TemplateProvider templateProvider = new TemplateProviderImpl();
        KnativeBroker broker = templateProvider.loadBridgeIngressBrokerTemplate(hasMetadata, TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME));

        assertOwnerReference(hasMetadata, broker.getMetadata());
        assertLabels(broker.getMetadata(), LabelsBuilder.V2_OPERATOR_NAME, BRIDGE_COMPONENT_NAME);
        assertThat(broker.getMetadata().getAnnotations().get("eventing.knative.dev/broker.class")).isEqualTo("Kafka");
        assertThat(broker.getMetadata().getAnnotations().get("kafka.eventing.knative.dev/external.topic")).isBlank();
        assertThat(broker.getSpec().getConfig().getKind()).isEqualTo("ConfigMap");
        assertThat(broker.getSpec().getConfig().getApiVersion()).isEqualTo("v1");
    }

    @Test
    public void bridgeIngressAuthorizationPolicyTemplateIsProvided() {

        mockResourceForNoOwnerReference();
        TemplateProvider templateProvider = new TemplateProviderImpl();
        AuthorizationPolicy authorizationPolicy = templateProvider.loadBridgeIngressAuthorizationPolicyTemplate(hasMetadata,
                new TemplateImportConfig(LabelsBuilder.V1_OPERATOR_NAME)
                        .withNameFromParent()
                        .withPrimaryResourceFromParent());

        assertThat(authorizationPolicy.getMetadata().getOwnerReferences()).isNull();
        assertThat(authorizationPolicy.getMetadata().getAnnotations().get("operator-sdk/primary-resource-name")).isEqualTo(hasMetadata.getMetadata().getName());
        assertThat(authorizationPolicy.getMetadata().getAnnotations().get("operator-sdk/primary-resource-namespace")).isEqualTo(hasMetadata.getMetadata().getNamespace());
        assertLabels(authorizationPolicy.getMetadata(), LabelsBuilder.V1_OPERATOR_NAME, BRIDGE_COMPONENT_NAME);

        // account_id
        assertThat(authorizationPolicy.getSpec().getAction()).isEqualTo("ALLOW");
        assertThat(authorizationPolicy.getSpec().getRules().get(0).getTo().get(0).getOperation().getPaths().get(0)).isEqualTo("");
        assertThat(authorizationPolicy.getSpec().getRules().get(0).getTo().get(0).getOperation().getMethods().get(0)).isEqualTo("POST");
        assertThat(authorizationPolicy.getSpec().getRules().get(0).getTo().get(0).getOperation().getMethods().get(1)).isEqualTo("OPTIONS");
        assertThat(authorizationPolicy.getSpec().getRules().get(0).getWhen().get(0).getKey()).isEqualTo("request.auth.claims[account_id]");
        assertThat(authorizationPolicy.getSpec().getRules().get(0).getWhen().get(0).getValues().get(0)).isBlank();

        // rh-user-id
        assertThat(authorizationPolicy.getSpec().getRules().get(1).getTo().get(0).getOperation().getPaths().get(0)).isEqualTo("");
        assertThat(authorizationPolicy.getSpec().getRules().get(1).getTo().get(0).getOperation().getMethods().get(0)).isEqualTo("POST");
        assertThat(authorizationPolicy.getSpec().getRules().get(1).getTo().get(0).getOperation().getMethods().get(1)).isEqualTo("OPTIONS");
        assertThat(authorizationPolicy.getSpec().getRules().get(1).getWhen().get(0).getKey()).isEqualTo("request.auth.claims[rh-user-id]");
        assertThat(authorizationPolicy.getSpec().getRules().get(1).getWhen().get(0).getValues().get(0)).isBlank(); // customerId
        assertThat(authorizationPolicy.getSpec().getRules().get(1).getWhen().get(0).getValues().get(1)).isBlank(); // webhook technical accountId
    }

    @Test
    public void bridgeIngressConfigMapTemplateIsProvided() {

        mockResourceForOwnerReference();
        TemplateProvider templateProvider = new TemplateProviderImpl();
        ConfigMap configMap = templateProvider.loadBridgeIngressConfigMapTemplate(hasMetadata, TemplateImportConfig.withDefaults(LabelsBuilder.V1_OPERATOR_NAME));

        assertOwnerReference(hasMetadata, configMap.getMetadata());
        assertLabels(configMap.getMetadata(), LabelsBuilder.V1_OPERATOR_NAME, BRIDGE_COMPONENT_NAME);

        assertThat(configMap.getData().get("default.topic.partitions")).isBlank();
        assertThat(configMap.getData().get("default.topic.replication.factor")).isBlank();
        assertThat(configMap.getData().get("bootstrap.servers")).isBlank();
        assertThat(configMap.getData().get("auth.secret.ref.name")).isBlank();
        assertThat(configMap.getData().get("topic.name")).isBlank();
    }

    @Test
    public void bridgeIngressSecretTemplateIsProvided() {

        mockResourceForOwnerReference();
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Secret secret = templateProvider.loadBridgeIngressSecretTemplate(hasMetadata, TemplateImportConfig.withDefaults(LabelsBuilder.V2_OPERATOR_NAME));

        assertOwnerReference(hasMetadata, secret.getMetadata());
        assertLabels(secret.getMetadata(), LabelsBuilder.V2_OPERATOR_NAME, BRIDGE_COMPONENT_NAME);

        assertThat(secret.getData().get("protocol")).isBlank();
        assertThat(secret.getData().get("sasl.mechanism")).isBlank();
        assertThat(secret.getData().get("user")).isBlank();
        assertThat(secret.getData().get("password")).isBlank();
        assertThat(secret.getData().get("bootstrap.servers")).isBlank();
        assertThat(secret.getData().get("topic.name")).isBlank();
    }

    @Test
    public void bridgeIngressOpenshiftRouteTemplateIsProvided() {

        mockResourceForNoOwnerReference();
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Route route = templateProvider.loadBridgeIngressOpenshiftRouteTemplate(hasMetadata, new TemplateImportConfig(LabelsBuilder.V1_OPERATOR_NAME)
                .withNameFromParent()
                .withPrimaryResourceFromParent());

        assertThat(route.getMetadata().getOwnerReferences()).isNull();
        assertLabels(route.getMetadata(), LabelsBuilder.V1_OPERATOR_NAME, BRIDGE_COMPONENT_NAME);
        assertThat(route.getSpec().getTo().getKind()).isEqualTo("Service");
        assertThat(route.getSpec().getPort().getTargetPort().getStrVal()).isEqualTo("http2");
    }

    @Test
    public void bridgeIngressKubernetesIngressTemplateIsProvided() {

        mockResourceForNoOwnerReference();
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Ingress ingress = templateProvider.loadBridgeIngressKubernetesIngressTemplate(hasMetadata, new TemplateImportConfig(LabelsBuilder.V1_OPERATOR_NAME)
                .withNameFromParent()
                .withPrimaryResourceFromParent()
                .withOperatorName(LabelsBuilder.V1_OPERATOR_NAME));

        assertThat(ingress.getMetadata().getOwnerReferences()).isNull();
        assertLabels(ingress.getMetadata(), LabelsBuilder.V1_OPERATOR_NAME, BRIDGE_COMPONENT_NAME);

        assertThat(ingress.getSpec().getRules().size()).isEqualTo(1);
        assertThat(ingress.getSpec().getRules().get(0).getHttp().getPaths().size()).isEqualTo(1);
        assertThat(ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPathType()).isEqualTo(NetworkingConstants.K8S_INGRESS_PATH_TYPE);
    }

    private void assertLabels(ObjectMeta meta, String operatorName, String component) {
        assertThat(meta.getLabels().get(LabelsBuilder.COMPONENT_LABEL)).isEqualTo(component);
        assertThat(meta.getLabels().get(LabelsBuilder.MANAGED_BY_LABEL)).isEqualTo(operatorName);
        assertThat(meta.getLabels().get(LabelsBuilder.CREATED_BY_LABEL)).isEqualTo(operatorName);
    }

    private void assertOwnerReference(HasMetadata resource, ObjectMeta meta) {
        assertThat(meta.getOwnerReferences().size()).isEqualTo(1);

        OwnerReference ownerReference = meta.getOwnerReferences().get(0);
        assertThat(ownerReference.getName()).isEqualTo(resource.getMetadata().getName());
        assertThat(ownerReference.getApiVersion()).isEqualTo(resource.getApiVersion());
        assertThat(ownerReference.getKind()).isEqualTo(resource.getKind());
        assertThat(ownerReference.getUid()).isEqualTo(resource.getMetadata().getUid());
    }
}
