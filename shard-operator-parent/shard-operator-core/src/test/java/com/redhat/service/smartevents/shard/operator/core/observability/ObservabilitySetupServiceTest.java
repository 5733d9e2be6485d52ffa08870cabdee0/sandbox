package com.redhat.service.smartevents.shard.operator.core.observability;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObservabilitySetupServiceTest {

    private static String NAME = "name";
    private static String NAMESPACE = "namespace";
    private static String ACCESS_TOKEN = "access-token";
    private static String REPOSITORY = "repository";
    private static String CHANNEL = "channel";
    private static String TAG = "tag";

    private static Base64.Encoder ENCODER = Base64.getEncoder();

    @Mock
    KubernetesClient client;

    @Mock
    TemplateProvider templateProvider;

    @Mock
    MixedOperation<Secret, SecretList, Resource<Secret>> secrets;

    @Mock
    NonNamespaceOperation<Secret, SecretList, Resource<Secret>> secretsNonNamespaceOperation;

    @Mock
    Resource<Secret> secretResource;

    @Mock
    NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespaceNonNamespaceOperation;

    @Mock
    Resource<Namespace> namespaceResource;

    ObservabilitySetupServiceForTests handler;

    @BeforeEach
    void setup() {
        this.handler = new ObservabilitySetupServiceForTests(client, templateProvider);
    }

    @Test
    void testDisabled() {
        handler.setEnabled(false);

        handler.createOrUpdateObservabilitySecret();

        verifyNoInteractions(client);
        verifyNoInteractions(templateProvider);
    }

    @ParameterizedTest()
    @MethodSource("parameters")
    void testMissingParameter(String name, String namespace, String accessToken, String repository, String channel, String tag) {
        handler.setEnabled(true);

        handler.setName(name);
        handler.setNamespace(namespace);
        handler.setAccessToken(accessToken);
        handler.setRepository(repository);
        handler.setChannel(channel);
        handler.setTag(tag);

        handler.createOrUpdateObservabilitySecret();

        verifyNoInteractions(client);
    }

    private static Stream<Arguments> parameters() {
        Object[][] parameters = {
                { null, NAMESPACE, ACCESS_TOKEN, REPOSITORY, CHANNEL, TAG },
                { NAME, null, ACCESS_TOKEN, REPOSITORY, CHANNEL, TAG },
                { NAME, NAMESPACE, null, REPOSITORY, CHANNEL, TAG },
                { NAME, NAMESPACE, ACCESS_TOKEN, null, CHANNEL, TAG },
                { NAME, NAMESPACE, ACCESS_TOKEN, REPOSITORY, null, TAG },
                { NAME, NAMESPACE, ACCESS_TOKEN, REPOSITORY, CHANNEL, null }
        };
        return Stream.of(parameters).map(Arguments::of);
    }

    @Test
    void testCompleteSetupWithExistingTargetNamespace() {
        handler.setEnabled(true);

        handler.setName(NAME);
        handler.setNamespace(NAMESPACE);
        handler.setAccessToken(ACCESS_TOKEN);
        handler.setRepository(REPOSITORY);
        handler.setChannel(CHANNEL);
        handler.setTag(TAG);

        stubKubernetes();
        stubTemplateProvider();
        when(namespaceResource.get()).thenReturn(new Namespace());

        handler.createOrUpdateObservabilitySecret();

        verifySecretCreation();
    }

    @Test
    void testCompleteSetupWithoutExistingTargetNamespace() {
        handler.setEnabled(true);

        handler.setName(NAME);
        handler.setNamespace(NAMESPACE);
        handler.setAccessToken(ACCESS_TOKEN);
        handler.setRepository(REPOSITORY);
        handler.setChannel(CHANNEL);
        handler.setTag(TAG);

        stubKubernetes();
        stubTemplateProvider();

        handler.createOrUpdateObservabilitySecret();

        verifyNamespaceCreation();
        verifySecretCreation();
    }

    private void stubKubernetes() {
        when(client.secrets()).thenReturn(secrets);
        when(secrets.inNamespace(anyString())).thenReturn(secretsNonNamespaceOperation);
        when(secretsNonNamespaceOperation.withName(anyString())).thenReturn(secretResource);

        when(client.namespaces()).thenReturn(namespaceNonNamespaceOperation);
        when(namespaceNonNamespaceOperation.withName(anyString())).thenReturn(namespaceResource);
    }

    private void stubTemplateProvider() {
        Secret secret = new Secret();
        secret.setData(new HashMap<>());
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName("name");
        metadata.setNamespace("namespace");
        secret.setMetadata(metadata);
        when(templateProvider.loadObservabilitySecretTemplate("name", "namespace")).thenReturn(secret);
    }

    private void verifyNamespaceCreation() {
        ArgumentCaptor<Namespace> namespaceArgumentCaptor = ArgumentCaptor.forClass(Namespace.class);

        verify(namespaceNonNamespaceOperation).createOrReplace(namespaceArgumentCaptor.capture());

        Namespace namespace = namespaceArgumentCaptor.getValue();
        assertThat(namespace).isNotNull();

        assertThat(namespace.getMetadata().getName()).isEqualTo(NAMESPACE);
    }

    private void verifySecretCreation() {
        ArgumentCaptor<Secret> secretArgumentCaptor = ArgumentCaptor.forClass(Secret.class);

        verify(secretResource).createOrReplace(secretArgumentCaptor.capture());

        Secret secret = secretArgumentCaptor.getValue();
        assertThat(secret).isNotNull();

        assertThat(secret.getData()).hasSize(4);
        assertThat(secret.getData()).containsEntry(ObservabilitySetupServiceForTests.OBSERVABILITY_ACCESS_TOKEN,
                ENCODER.encodeToString(ACCESS_TOKEN.getBytes(StandardCharsets.UTF_8)));
        assertThat(secret.getData()).containsEntry(ObservabilitySetupServiceForTests.OBSERVABILITY_REPOSITORY,
                ENCODER.encodeToString(REPOSITORY.getBytes(StandardCharsets.UTF_8)));
        assertThat(secret.getData()).containsEntry(ObservabilitySetupServiceForTests.OBSERVABILITY_CHANNEL,
                ENCODER.encodeToString(CHANNEL.getBytes(StandardCharsets.UTF_8)));
        assertThat(secret.getData()).containsEntry(ObservabilitySetupServiceForTests.OBSERVABILITY_TAG,
                ENCODER.encodeToString(TAG.getBytes(StandardCharsets.UTF_8)));
    }

}
