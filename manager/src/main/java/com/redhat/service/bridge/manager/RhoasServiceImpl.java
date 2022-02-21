package com.redhat.service.bridge.manager;

import java.time.Duration;
import java.util.concurrent.CompletionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.Topic;
import com.openshift.cloud.api.kas.auth.models.TopicSettings;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.bridge.rhoas.RhoasClient;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import io.smallrye.mutiny.TimeoutException;

@ApplicationScoped
public class RhoasServiceImpl implements RhoasService {

    @ConfigProperty(name = "rhoas.timeout-seconds")
    int rhoasTimeout;
    @ConfigProperty(name = "rhoas.ops-account.client-id")
    String rhoasOpsAccountClientId;

    @Inject
    RhoasClient rhoasClient;

    @Override
    public Topic createTopicAndGrantAccessFor(String topicName, RhoasTopicAccessType accessType) {
        try {
            NewTopicInput newTopicInput = new NewTopicInput()
                    .name(topicName)
                    .settings(new TopicSettings().numPartitions(1));

            return rhoasClient.createTopicAndGrantAccess(newTopicInput, rhoasOpsAccountClientId, accessType).await().atMost(Duration.ofSeconds(rhoasTimeout));
        } catch (CompletionException e) {
            throw new InternalPlatformException(createFailureErrorMessageFor(topicName), e);
        } catch (TimeoutException e) {
            throw new InternalPlatformException(createTimeoutErrorMessageFor(topicName), e);
        }
    }

    @Override
    public void deleteTopicAndRevokeAccessFor(String topicName, RhoasTopicAccessType accessType) {
        try {
            rhoasClient.deleteTopicAndRevokeAccess(topicName, rhoasOpsAccountClientId, accessType)
                    .await().atMost(Duration.ofSeconds(rhoasTimeout));
        } catch (CompletionException e) {

            throw new InternalPlatformException(deleteFailureErrorMessageFor(topicName), e);
        } catch (TimeoutException e) {
            throw new InternalPlatformException(deleteTimeoutErrorMessageFor(topicName), e);
        }
    }

    public static String createFailureErrorMessageFor(String topicName) {
        return String.format("Failed creating and granting access to topic '%s'", topicName);
    }

    static String createTimeoutErrorMessageFor(String topicName) {
        return String.format("Timeout reached while creating topic and granting access to topic '%s'", topicName);
    }

    static String deleteFailureErrorMessageFor(String topicName) {
        return String.format("Failed deleting topic and revoking access from topic '%s'", topicName);
    }

    static String deleteTimeoutErrorMessageFor(String topicName) {
        return String.format("Timeout reached while deleting topic and revoking access from topic '%s'", topicName);
    }
}
