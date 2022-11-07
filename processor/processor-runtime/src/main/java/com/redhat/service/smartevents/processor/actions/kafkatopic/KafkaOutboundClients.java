package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;

import io.smallrye.common.annotation.Identifier;

@ApplicationScoped
public class KafkaOutboundClients {

    @Inject
    Instance<ProcessorDTO> optProcessorDTO;

    // See https://quarkus.io/guides/kafka#kafka-configuration-resolution
    // Attribute values are resolved as follows:
    // the attribute is set directly on the channel configuration (mp.messaging.incoming.my-channel.attribute=value),
    // if not set, the connector looks for a Map with the channel name or the configured kafka-configuration (if set) and the value is retrieved from that Map
    // If the resolved Map does not contain the value the default Map is used (exposed with the default-kafka-broker name)
    //
    // Therefore if this map doesn't have the value, the default kafka client is used
    @Produces
    @ApplicationScoped
    @Identifier("actions-out")
    Map<String, Object> outgoing() {
        if (optProcessorDTO.isUnsatisfied()) {
            return Collections.emptyMap();
        }

        ProcessorDTO processorDTO = optProcessorDTO.get();

        if (processorDTO.getDefinition() == null || processorDTO.getDefinition().getResolvedAction() == null) {
            return Collections.emptyMap();
        }

        Action action = processorDTO.getDefinition().getResolvedAction();

        String brokerUrl = action.getParameter(KafkaTopicAction.BROKER_URL);
        String clientId = action.getParameter(KafkaTopicAction.CLIENT_ID);
        String clientSecret = action.getParameter(KafkaTopicAction.CLIENT_SECRET);
        String securityProtocol = action.getParameter(KafkaTopicAction.SECURITY_PROTOCOL);

        if (brokerUrl == null || clientId == null || clientSecret == null) {
            return Collections.emptyMap();
        }

        return Map.ofEntries(
                Map.entry("bootstrap.servers", brokerUrl),
                Map.entry("sasl.mechanism", "PLAIN"),
                Map.entry("security.protocol", securityProtocol),
                Map.entry("sasl.jaas.config",
                        String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                                clientId, clientSecret)));
    }
}
