package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.Processing;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

import static com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor.OB_RESOURCE_NAME_PREFIX;

@Version("v1")
@Group("camel.apache.org")
@Kind("Integration")
public class CamelIntegration extends CustomResource<CamelIntegrationSpec, CamelIntegrationStatus> implements Namespaced {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelIntegration.class);

    public static String resolveResourceName(String id) {
        return String.format("%scamel-%s", OB_RESOURCE_NAME_PREFIX, KubernetesResourceUtil.sanitizeName(id));
    }

    public static CamelIntegration fromDTO(ProcessorDTO processorDTO, String namespace, String executorImage, Processing processing) {

        LOGGER.info("------ fromDto: " + processorDTO);

        ObjectMeta metadata = new ObjectMetaBuilder()
                .withName(resolveResourceName(processorDTO.getId()))
                .withNamespace(namespace)
                .withLabels(new LabelsBuilder()
                        .buildWithDefaults())
                .build();

        LOGGER.info("------ metadata: " + metadata);

        CamelIntegrationFlow camelIntegrationFlow = new CamelIntegrationFlow();

        CamelIntegrationKafkaConnectionFrom camelIntegrationFrom = new CamelIntegrationKafkaConnectionFrom();

        camelIntegrationFlow.setFrom(camelIntegrationFrom);

        String bootstrapServers = processorDTO.getKafkaConnection().getBootstrapServers();
        String topic = processorDTO.getKafkaConnection().getTopic();

        camelIntegrationFrom.setUri(String.format("kafka:%s", topic));

        camelIntegrationFrom.setParameters(kafkaConnectionsParameter(processorDTO, bootstrapServers));

        LOGGER.info("------ camelIntegrationFrom: " + camelIntegrationFrom);

        CamelIntegrationSpec camelIntegrationSpec = new CamelIntegrationSpec();

        camelIntegrationSpec.setFlows(Collections.singletonList(camelIntegrationFlow));

        CamelIntegration camelIntegration = new CamelIntegration();

        camelIntegration.setSpec(camelIntegrationSpec);
        camelIntegration.setMetadata(metadata);

        CamelIntegrationTo camelIntegrationTo = new CamelIntegrationTo();

        ObjectNode spec = processing.getSpec();

        String toLabel = spec.get("flow").get("from").get("steps").get(0).get("to").asText();

        LOGGER.info("------ toLabel: " + toLabel);

        Optional<Action> action = processorDTO.getDefinition()
                .getMultipleActions()
                .stream().filter(n -> {
                    LOGGER.info("------ action name: " + n.getName());
                    return n.getName().equals(toLabel);
                }).findFirst();

        action.ifPresent(a -> {
            String toTopic = a.getParameter("topic");

            CamelIntegrationKafkaConnectionTo to = new CamelIntegrationKafkaConnectionTo();
            to.setParameters(kafkaConnectionsParameter(processorDTO, bootstrapServers));

            String kafkaToURI = String.format("kafka:%s", toTopic);
            to.setUri(kafkaToURI);
            camelIntegrationTo.setTo(to);
            camelIntegrationFrom.getSteps().add(camelIntegrationTo);
        });

        LOGGER.info("------ camelIntegration: " + camelIntegration);

        return camelIntegration;
    }

    // TODO CAMEL-POC secrets
    private static Map<String, Object> kafkaConnectionsParameter(ProcessorDTO processorDTO, String bootstrapServers) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("brokers", bootstrapServers);
        parameters.put("securityProtocol", "SASL_SSL");
        parameters.put("saslMechanism", "PLAIN");
        parameters.put("saslJaasConfig",
                String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
                        processorDTO.getKafkaConnection().getClientId(), processorDTO.getKafkaConnection().getClientSecret()));
        parameters.put("maxPollRecords", 5000);
        parameters.put("consumersCount", 1);
        parameters.put("seekTo", "beginning");
        parameters.put("groupId", "kafkaGroup");
        return parameters;
    }

    @Override
    public String toString() {
        return "CamelIntegration{" +
                "spec=" + spec +
                ", status=" + status +
                '}';
    }
}
