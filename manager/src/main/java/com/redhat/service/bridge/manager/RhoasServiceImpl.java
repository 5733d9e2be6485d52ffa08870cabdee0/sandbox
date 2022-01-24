package com.redhat.service.bridge.manager;

import java.time.Duration;
import java.util.concurrent.CompletionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.TopicSettings;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.bridge.rhoas.RhoasClient;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.smallrye.mutiny.TimeoutException;

import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG;
import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG_DEFAULT_VALUE;

@ApplicationScoped
public class RhoasServiceImpl implements RhoasService {

    @ConfigProperty(name = ENABLED_FLAG, defaultValue = ENABLED_FLAG_DEFAULT_VALUE)
    boolean rhoasEnabled;
    @ConfigProperty(name = "rhoas.timeout-seconds")
    int rhoasTimeout;
    @ConfigProperty(name = "rhoas.ops-account.client-id")
    String rhoasOpsAccountClientId;

    @Inject
    RhoasClient rhoasClient;

    @Override
    public boolean isEnabled() {
        return rhoasEnabled;
    }

    @Override
    public String createTopicAndGrantAccessForBridge(String bridgeId, RhoasTopicAccessType accessType) {
        return createTopicAndGrantAccessFor("bridge", bridgeId, accessType);
    }

    @Override
    public void deleteTopicAndRevokeAccessForBridge(String bridgeId, RhoasTopicAccessType accessType) {
        deleteTopicAndRevokeAccessFor("bridge", bridgeId, accessType);
    }

    @Override
    public String createTopicAndGrantAccessForProcessor(String processorId, RhoasTopicAccessType accessType) {
        return createTopicAndGrantAccessFor("processor", processorId, accessType);
    }

    @Override
    public void deleteTopicAndRevokeAccessForProcessor(String processorId, RhoasTopicAccessType accessType) {
        deleteTopicAndRevokeAccessFor("processor", processorId, accessType);
    }

    private String createTopicAndGrantAccessFor(String entityType, String entityId, RhoasTopicAccessType accessType) {
        if (!rhoasEnabled) {
            throw new IllegalStateException("RHOAS integration is disabled");
        }
        try {
            String newTopicName = topicNameFor(entityId);
            NewTopicInput newTopicInput = new NewTopicInput()
                    .name(newTopicName)
                    .settings(new TopicSettings().numPartitions(1));

            rhoasClient.createTopicAndGrantAccess(newTopicInput, rhoasOpsAccountClientId, accessType)
                    .await().atMost(Duration.ofSeconds(rhoasTimeout));

            return newTopicName;
        } catch (CompletionException e) {
            String msg = String.format("Failed creating topic and granting access for %s '%s'", entityType, entityId);
            throw new InternalPlatformException(msg, e);
        } catch (TimeoutException e) {
            String msg = String.format("Timeout reached while creating topic and granting access for %s '%s'", entityType, entityId);
            throw new InternalPlatformException(msg, e);
        }
    }

    private void deleteTopicAndRevokeAccessFor(String entityType, String entityId, RhoasTopicAccessType accessType) {
        if (!rhoasEnabled) {
            throw new IllegalStateException("RHOAS integration is disabled");
        }
        try {
            rhoasClient.deleteTopicAndRevokeAccess(topicNameFor(entityId), rhoasOpsAccountClientId, accessType)
                    .await().atMost(Duration.ofSeconds(rhoasTimeout));
        } catch (CompletionException e) {
            String msg = String.format("Failed deleting topic and revoking access for %s '%s'", entityType, entityId);
            throw new InternalPlatformException(msg, e);
        } catch (TimeoutException e) {
            String msg = String.format("Timeout reached while deleting topic and revoking access for %s '%s'", entityType, entityId);
            throw new InternalPlatformException(msg, e);
        }
    }

    private String topicNameFor(String bridgeId) {
        return String.format("ob-%s", bridgeId);
    }
}
