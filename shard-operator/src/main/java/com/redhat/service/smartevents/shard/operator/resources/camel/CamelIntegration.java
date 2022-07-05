package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutorStatus;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;
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
public class CamelIntegration extends CustomResource<CamelIntegrationSpec, CamelIntegrationStatus> {

    public static final String COMPONENT_NAME = "integration";

    private static final ObjectMapper MAPPER = new ObjectMapper();


    public static String resolveResourceName(String id) {
        return String.format("%scamel-%s", OB_RESOURCE_NAME_PREFIX, KubernetesResourceUtil.sanitizeName(id));
    }

    public static CamelIntegration fromDTO(ProcessorDTO processorDTO, String namespace, String executorImage) {

        ObjectMeta metadata = new ObjectMetaBuilder()
                .withName(resolveResourceName(processorDTO.getId()))
                .withNamespace(namespace)
                .withLabels(new LabelsBuilder()
                                    .withComponent(COMPONENT_NAME)
                                    .buildWithDefaults())
                .build();

        ObjectNode spec = processorDTO.getDefinition().getProcessing().getSpec();

        CamelIntegrationFlows camelIntegrationFlows = new CamelIntegrationFlows();

        CamelIntegrationFrom camelIntegrationFrom = new CamelIntegrationFrom();

        String bootstrapServers = processorDTO.getKafkaConnection().getBootstrapServers();
        String topic = processorDTO.getKafkaConnection().getTopic();

        camelIntegrationFrom.setUri(String.format("kafka:%s", topic));

        // TODO CAMEL-POC secrets
        camelIntegrationFrom.getParameters().put("brokers", bootstrapServers);
        camelIntegrationFrom.getParameters().put("securityProtocol", "SASL_SSL");
        camelIntegrationFrom.getParameters().put("saslMechanism", "PLAIN");
        camelIntegrationFrom.getParameters().put("saslJaasConfig",
                                                 String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username='%s' password='%s';",
                                                               processorDTO.getKafkaConnection().getClientId(), processorDTO.getKafkaConnection().getClientSecret()));
        camelIntegrationFrom.getParameters().put("maxPollRecords", 5000);
        camelIntegrationFrom.getParameters().put("consumersCount", 1);
        camelIntegrationFrom.getParameters().put("seekTo", "beginning");
        camelIntegrationFrom.getParameters().put("groupId", "kafkaGroup");

        camelIntegrationFlows.setCamelIntegrationFrom(Collections.singletonList(camelIntegrationFrom));

        CamelIntegrationSpec camelIntegrationSpec = new CamelIntegrationSpec();

        camelIntegrationSpec.setCamelIntegrationFlows(camelIntegrationFlows);

        CamelIntegration camelIntegration = new CamelIntegration();

        camelIntegration.setSpec(camelIntegrationSpec);
        camelIntegration.setMetadata(metadata);

        CamelIntegrationTo camelIntegrationTo = new CamelIntegrationTo();
        String kafkaToURI = String.format("kafka:%s", processorDTO.getDefinition().getResolvedAction().getParameter("topic"));
        camelIntegrationTo.setTo(kafkaToURI);
        camelIntegrationFrom.getSteps().add(camelIntegrationTo);


        return camelIntegration;
    }
}
