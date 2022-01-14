package com.redhat.service.bridge.manager.actions.connectors;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.TopicSettings;
import com.redhat.service.bridge.actions.ActionTransformer;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.rhoas.RhoasClient;

import io.smallrye.mutiny.TimeoutException;

import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG;
import static com.redhat.service.bridge.rhoas.RhoasProperties.ENABLED_FLAG_DEFAULT_VALUE;

@ApplicationScoped
public class SlackActionTransformer implements ActionTransformer {

    @ConfigProperty(name = "managed-connectors.topic-name")
    String topicName;

    @ConfigProperty(name = ENABLED_FLAG, defaultValue = ENABLED_FLAG_DEFAULT_VALUE)
    boolean rhoasEnabled;
    @ConfigProperty(name = "rhoas.timeout-seconds")
    int rhoasTimeout;
    @Inject
    RhoasClient rhoasClient;

    @Override
    public BaseAction transform(BaseAction action, String bridgeId, String customerId, String processorId) {

        BaseAction resolvedAction = new BaseAction();

        Map<String, String> newParameters = resolvedAction.getParameters();
        newParameters.putAll(action.getParameters());

        resolvedAction.setType(KafkaTopicAction.TYPE);
        resolvedAction.setName(action.getName());

        newParameters.put(KafkaTopicAction.TOPIC_PARAM, generateKafkaTopicName(processorId));

        return resolvedAction;
    }

    // Currently, it's just a fixed topic for testing purposes.
    // When https://issues.redhat.com/browse/MGDOBR-168 is ready, we can generate one for connector
    // once we use a single topic for every connector there will be no need of having a different
    // one per connector https://issues.redhat.com/browse/MGDSTRM-5977
    private String generateKafkaTopicName(String processorId) {
        System.err.println("-----------> UUUUUUUU " + rhoasEnabled);
        if (!rhoasEnabled) {
            return topicName;
        }
        NewTopicInput request = new NewTopicInput()
                .name(String.format("ob-%s", processorId))
                .settings(new TopicSettings().numPartitions(1));
        try {
            return rhoasClient.createTopic(request).await().atMost(Duration.ofSeconds(rhoasTimeout)).getName();
        } catch (CompletionException e) {
            String msg = "Failed creating topic for processor " + processorId;
            throw new InternalPlatformException(msg, e);
        } catch (TimeoutException e) {
            String msg = "Timeout reached while creating topic for processor " + processorId;
            throw new InternalPlatformException(msg, e);
        }
    }
}
