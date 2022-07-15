package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.Processing;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

import static com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor.OB_RESOURCE_NAME_PREFIX;

@Version("v1")
@Group("camel.apache.org")
@Kind("Integration")
public class CamelIntegration extends CustomResource<ObjectNode, CamelIntegrationStatus> implements Namespaced {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelIntegration.class);

    public static String resolveResourceName(String id) {
        return String.format("%scamel-%s", OB_RESOURCE_NAME_PREFIX, KubernetesResourceUtil.sanitizeName(id));
    }

    public static CamelIntegration fromDTO(ProcessorDTO processorDTO, String namespace, Processing processing, Secret secret) {

        LOGGER.info("------ fromDto: " + processorDTO);

        String name = resolveResourceName(processorDTO.getId());
        ObjectMeta metadata = new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(namespace)
                .withLabels(new LabelsBuilder()
                        .buildWithDefaults())
                .build();

        LOGGER.info("------ metadata: " + metadata);

        CamelIntegration camelIntegration = new CamelIntegration();
        if (secret == null) {
            return camelIntegration;
        }

        camelIntegration.setMetadata(metadata);

        ObjectNode spec = MAPPER.createObjectNode();
        camelIntegration.setSpec(spec);

        ArrayNode flows = MAPPER.createArrayNode();
        spec.set("flows", flows);

        ObjectNode flow = MAPPER.createObjectNode();
        flows.add(flow);

        ObjectNode from = MAPPER.createObjectNode();
        flow.set("from", from);

        ObjectNode inputSpec = processing.getSpec();
        ArrayNode steps = getFlowSteps(inputSpec);
        from.set("steps", steps);

        TextNode value = bridgeTopic(secret);
        from.set("uri", value);

        ObjectNode fromParameters = MAPPER.createObjectNode();
        Map<String, JsonNode> fromKafkaParameters = transformToJsonMap(secret);
        fromParameters.setAll(fromKafkaParameters);
        from.set("parameters", fromParameters);

        List<JsonNode> tos = inputSpec.findValues("to");

        for (JsonNode to : tos) {
            List<Action> resolvedActions = processorDTO.getDefinition().getResolvedActions();
            replaceToWithKafkaConnectionParameters(to, resolvedActions, fromKafkaParameters);
        }

        LOGGER.info("------ camelIntegration: " + camelIntegration);

        return camelIntegration;
    }

    private static void replaceToWithKafkaConnectionParameters(JsonNode to, List<Action> resolvedActions, Map<String, JsonNode> kafkaParameters) {
        if (to instanceof ObjectNode) {
            ObjectNode toObjectNode = (ObjectNode) to;
            String toText = toObjectNode.get("uri").asText();

            Optional<ObjectNode> optKafkaTo = convertTo(toText, resolvedActions, kafkaParameters);

            optKafkaTo.ifPresent(kafkaToElement -> {
                JsonNode kafkaTo = kafkaToElement.get("to");
                toObjectNode.set("uri", kafkaTo.get("uri"));
                toObjectNode.set("parameters", kafkaTo.get("parameters"));
            });

            if (optKafkaTo.isEmpty()) { // assume it's error
                JsonNode steps1 = to.findParent("steps");
                System.out.println(steps1);
            }
        }
    }

    private static Optional<ObjectNode> convertTo(String toLabel,
            List<Action> resolvedActions,
            Map<String, JsonNode> kafkaParameters) {

        Optional<Action> action = findActionWithLabel(toLabel, resolvedActions);

        LOGGER.info("---- Action for label {}: {}", toLabel, action);

        return action.map(a -> {
            String actionTopic = a.getParameter("topic");

            ObjectNode to = MAPPER.createObjectNode();

            ObjectNode fromParameters = MAPPER.createObjectNode();
            fromParameters.setAll(kafkaParameters);
            to.set("parameters", fromParameters);

            String kafkaToURI = String.format("kafka:%s", actionTopic);
            to.set("uri", new TextNode(kafkaToURI));

            ObjectNode toStep = MAPPER.createObjectNode();
            toStep.set("to", to);
            return toStep;
        });
    }

    private static Optional<Action> findActionWithLabel(String toLabel, List<Action> resolvedActions) {
        return resolvedActions
                .stream().filter(n -> {
                    LOGGER.info("------ action name: " + n.getName());
                    return n.getName().equals(toLabel);
                }).findFirst();
    }

    // TODO CAMEL-POC secrets
    private static Map<String, Object> kafkaConnectionsParameter(Secret secret) {
        if (secret == null) {
            return Collections.emptyMap();
        }

        String bootstrapServers = decodeBase64(secret, GlobalConfigurationsConstants.KAFKA_BOOTSTRAP_SERVERS_ENV_VAR);
        String clientId = decodeBase64(secret, GlobalConfigurationsConstants.KAFKA_CLIENT_ID_ENV_VAR);
        String clientSecret = decodeBase64(secret, GlobalConfigurationsConstants.KAFKA_CLIENT_SECRET_ENV_VAR);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("brokers", bootstrapServers);
        parameters.put("securityProtocol", "SASL_SSL");
        parameters.put("saslMechanism", "PLAIN");
        parameters.put("saslJaasConfig",
                String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
                        clientId, clientSecret));
        parameters.put("maxPollRecords", 5000);
        parameters.put("consumersCount", 1);
        parameters.put("seekTo", "beginning");
        parameters.put("groupId", "kafkaGroup");
        return parameters;
    }

    private static ArrayNode getFlowSteps(ObjectNode inputSpec) {
        ObjectNode inputSingleFlow = (ObjectNode) inputSpec.get("flow");
        return (ArrayNode) inputSingleFlow.get("from").get("steps");
    }

    private static TextNode bridgeTopic(Secret secret) {
        String topic = decodeBase64(secret, GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR);
        return new TextNode(String.format("kafka:%s", topic));
    }

    private static String decodeBase64(Secret secret, String key) {
        return new String(Base64.getDecoder().decode(secret.getData().get(key)));
    }

    private static Map<String, JsonNode> transformToJsonMap(Secret secret) {
        return kafkaConnectionsParameter(secret).entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Object value = e.getValue();
                    if (value instanceof Number) {
                        return new IntNode(Integer.parseInt(value.toString()));
                    }
                    return new TextNode(value.toString());
                }));
    }

    @Override
    public String toString() {
        return "CamelIntegration{" +
                "spec=" + (spec != null ? spec.toPrettyString() : spec) +
                ", status=" + status +
                '}';
    }
}
