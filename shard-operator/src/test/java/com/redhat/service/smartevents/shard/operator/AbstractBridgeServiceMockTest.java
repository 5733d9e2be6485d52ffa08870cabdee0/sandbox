package com.redhat.service.smartevents.shard.operator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.TemplateProvider;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceSpec;
import io.fabric8.kubernetes.api.model.NamespaceStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniSubscribe;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Heavily mocked base test class to avoid invoking k8s reconciliation loop.
 * 
 * @param <R> The Bridge resource to test.
 */
@ExtendWith(MockitoExtension.class)
abstract class AbstractBridgeServiceMockTest<R extends HasMetadata> {

    @Mock
    KubernetesClient kubernetesClient;

    @Mock
    ManagerClient managerClient;

    @Mock
    MixedOperation<R, KubernetesResourceList<R>, Resource<R>> k8sBridgeResources;

    @Mock
    NonNamespaceOperation<R, KubernetesResourceList<R>, Resource<R>> k8sBridgeNamespace;

    @Mock
    Resource<R> k8sBridgeResource;

    @Mock
    MixedOperation<Secret, SecretList, Resource<Secret>> k8sSecretsResources;

    @Mock
    NonNamespaceOperation<Secret, SecretList, Resource<Secret>> k8sSecretsNamespace;

    @Mock
    Resource<Secret> k8sSecretResource;

    @Mock
    Secret k8sSecret;

    @Mock
    CustomerNamespaceProvider customerNamespaceProvider;

    @Mock
    TemplateProvider templateProvider;

    @Mock
    Uni<HttpResponse<Buffer>> managerClientUniResponse;

    @Mock
    UniSubscribe<HttpResponse<Buffer>> managerClientUniSubscribeResponse;

    @BeforeEach
    public void setup() {
        ObjectMeta k8sMetadata = new ObjectMeta();
        k8sMetadata.setName("namespace");
        Namespace namespace = new Namespace("apiVersion", "kind", k8sMetadata, new NamespaceSpec(), new NamespaceStatus());

        when(customerNamespaceProvider.fetchOrCreateCustomerNamespace(anyString())).thenReturn(namespace);

        when(kubernetesClient.resources(getResourceClass())).thenReturn(k8sBridgeResources);
        when(k8sBridgeResources.inNamespace(anyString())).thenReturn(k8sBridgeNamespace);
        when(k8sBridgeNamespace.withName(anyString())).thenReturn(k8sBridgeResource);

        when(kubernetesClient.secrets()).thenReturn(k8sSecretsResources);
        when(k8sSecretsResources.inNamespace(anyString())).thenReturn(k8sSecretsNamespace);
        when(k8sSecretsNamespace.withName(anyString())).thenReturn(k8sSecretResource);
        when(k8sSecretResource.get()).thenReturn(k8sSecret);

        when(managerClientUniResponse.subscribe()).thenReturn(managerClientUniSubscribeResponse);

        setupService();
    }

    abstract Class<R> getResourceClass();

    abstract void setupService();

}
