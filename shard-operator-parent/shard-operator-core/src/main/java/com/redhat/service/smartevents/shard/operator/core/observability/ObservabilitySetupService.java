package com.redhat.service.smartevents.shard.operator.core.observability;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.shard.operator.core.SecretRestartHandler;
import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;

/**
 * Initialises Secret required by the Observability Operator in the Data Plane.
 * Changes to the underlying {@link ConfigProperty} values are handled by the CPaaS
 * AddOn deployment process and {@link SecretRestartHandler}.
 */
@ApplicationScoped
public class ObservabilitySetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilitySetupService.class);

    private static final String NAME_PARAMETER_NAME = "event-bridge.observability.name";
    private static final String NAMESPACE_PARAMETER_NAME = "event-bridge.observability.namespace";
    private static final String TOKEN_PARAMETER_NAME = "event-bridge.observability.access_token";
    private static final String REPOSITORY_PARAMETER_NAME = "event-bridge.observability.repository";
    private static final String CHANNEL_PARAMETER_NAME = "event-bridge.observability.channel";
    private static final String TAG_PARAMETER_NAME = "event-bridge.observability.tag";

    protected static final String OBSERVABILITY_ACCESS_TOKEN = "access_token";
    protected static final String OBSERVABILITY_REPOSITORY = "repository";
    protected static final String OBSERVABILITY_CHANNEL = "channel";
    protected static final String OBSERVABILITY_TAG = "tag";

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    @ConfigProperty(name = "event-bridge.observability.enabled")
    protected boolean enabled;

    protected String name;
    protected String namespace;
    protected String accessToken;
    protected String repository;
    protected String channel;
    protected String tag;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TemplateProvider templateProvider;

    void setupObservabilityComponent(@Observes StartupEvent event) {
        createOrUpdateObservabilitySecret();
    }

    protected void createOrUpdateObservabilitySecret() {
        if (!enabled) {
            LOGGER.info("Observability is disabled.");
            return;
        }

        try {
            name = getName();
            namespace = getNamespace();
            accessToken = getAccessToken();
            repository = getRepository();
            channel = getChannel();
            tag = getTag();

        } catch (NoSuchElementException nse) {
            LOGGER.error("One or more Observability parameters was undefined.");
            logParameters();
            // Mis-configuration is terminal as Observability must be running if enabled.
            Quarkus.asyncExit(1);
            return;
        }

        LOGGER.info("Observability is enabled.");
        logParameters();

        Secret existing = kubernetesClient
                .secrets()
                .inNamespace(namespace)
                .withName(name)
                .get();

        Secret expected = templateProvider.loadObservabilitySecretTemplate(name, namespace);
        expected.getData().put(OBSERVABILITY_ACCESS_TOKEN,
                ENCODER.encodeToString(accessToken.getBytes(StandardCharsets.UTF_8)));
        expected.getData().put(OBSERVABILITY_REPOSITORY,
                ENCODER.encodeToString(repository.getBytes(StandardCharsets.UTF_8)));
        expected.getData().put(OBSERVABILITY_CHANNEL,
                ENCODER.encodeToString(channel.getBytes(StandardCharsets.UTF_8)));
        expected.getData().put(OBSERVABILITY_TAG,
                ENCODER.encodeToString(tag.getBytes(StandardCharsets.UTF_8)));

        if (existing == null || !expected.getData().equals(existing.getData())) {
            LOGGER.info("Existing Observability Secret either does not exist or differs to that required. Creating...");
            try {
                // Ensure the target Namespace exists
                final Namespace ns = kubernetesClient.namespaces().withName(namespace).get();
                if (ns == null) {
                    LOGGER.info("Target Namespace for Observability Secret does not exist. Creating...");
                    kubernetesClient.namespaces().createOrReplace(
                            new NamespaceBuilder()
                                    .withNewMetadata()
                                    .withName(namespace)
                                    .withLabels(new LabelsBuilder().buildWithDefaults(LabelsBuilder.V1_OPERATOR_NAME))
                                    .endMetadata()
                                    .build());
                }

                // Ensure the Secret exists
                kubernetesClient
                        .secrets()
                        .inNamespace(expected.getMetadata().getNamespace())
                        .withName(expected.getMetadata().getName())
                        .createOrReplace(expected);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to create Observability Secret. Please make sure it was properly configured.");
                // Mis-configuration is terminal as Observability must be running if enabled.
                Quarkus.asyncExit(1);
            }
        }
    }

    private void logParameters() {
        LOGGER.debug("Name={}\n" +
                "Namespace={}\n" +
                "AccessToken={}\n" +
                "Repository={}\n" +
                "Channel={}\n" +
                "Tag={}",
                name, namespace, accessToken, repository, channel, tag);
    }

    protected String getName() {
        return ConfigProvider.getConfig().getValue(NAME_PARAMETER_NAME, String.class);
    }

    protected String getNamespace() {
        return ConfigProvider.getConfig().getValue(NAMESPACE_PARAMETER_NAME, String.class);
    }

    protected String getAccessToken() {
        return ConfigProvider.getConfig().getValue(TOKEN_PARAMETER_NAME, String.class);
    }

    protected String getRepository() {
        return ConfigProvider.getConfig().getValue(REPOSITORY_PARAMETER_NAME, String.class);
    }

    protected String getChannel() {
        return ConfigProvider.getConfig().getValue(CHANNEL_PARAMETER_NAME, String.class);
    }

    protected String getTag() {
        return ConfigProvider.getConfig().getValue(TAG_PARAMETER_NAME, String.class);
    }
}
