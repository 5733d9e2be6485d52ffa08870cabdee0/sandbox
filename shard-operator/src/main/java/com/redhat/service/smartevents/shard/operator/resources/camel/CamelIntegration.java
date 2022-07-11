package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.Base64;
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
public class CamelIntegration extends CustomResource<CamelIntegrationSpec, CamelIntegrationStatus> implements Namespaced {

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

        LOGGER.info("------ name: " + name);
        LOGGER.info("------ metadata: " + metadata);

        CamelIntegrationFlow camelIntegrationFlow = new CamelIntegrationFlow();

        CamelIntegrationKafkaConnectionFrom camelIntegrationFrom = new CamelIntegrationKafkaConnectionFrom();

        camelIntegrationFlow.setFrom(camelIntegrationFrom);

        if (secret != null) {
            String topic = decodeBase64(secret, GlobalConfigurationsConstants.KAFKA_TOPIC_ENV_VAR);
            camelIntegrationFrom.setUri(String.format("kafka:%s", topic));
        }

        camelIntegrationFrom.setParameters(kafkaConnectionsParameter(secret));

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
                .getResolvedActions()
                .stream().filter(n -> {
                    LOGGER.info("------ action name: " + n.getName());
                    return n.getName().equals(toLabel);
                }).findFirst();

        action.ifPresent(a -> {
            String toTopic = a.getParameter("topic");

            CamelIntegrationKafkaConnectionTo to = new CamelIntegrationKafkaConnectionTo();
            to.setParameters(kafkaConnectionsParameter(secret));

            String kafkaToURI = String.format("kafka:%s", toTopic);
            to.setUri(kafkaToURI);
            camelIntegrationTo.setTo(to);
            camelIntegrationFrom.getSteps().add(camelIntegrationTo);
        });

        LOGGER.info("------ camelIntegration: " + camelIntegration);

        return camelIntegration;
    }

    private static String decodeBase64(Secret secret, String key) {
        return new String(Base64.getDecoder().decode(secret.getData().get(key)));
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

    @Override
    public String toString() {
        return "CamelIntegration{" +
                "spec=" + spec +
                ", status=" + status +
                '}';
    }
}
